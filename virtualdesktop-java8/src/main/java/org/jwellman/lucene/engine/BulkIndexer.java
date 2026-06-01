package org.jwellman.lucene.engine;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.AlreadyClosedException;
import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.LogEntry;
import org.jwellman.lucene.model.SandboxRuntimeState;
import org.jwellman.lucene.model.SandboxRuntimeState.IndexStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Background Runnable that populates a Lucene index by scanning the configured source directory.
 *
 * <p>On first run the entire directory is indexed. On subsequent runs only files whose
 * {@code last_modified} timestamp exceeds the stored value are re-indexed; deleted files
 * are removed from the index. The writer is committed once after the full scan.</p>
 *
 * <p>All state updates go through {@link SandboxRuntimeState} volatile/Atomic fields so
 * the Swing sidebar reads them without synchronization.</p>
 */
public class BulkIndexer implements Runnable {

    private final IndexSandboxManager manager;

    public BulkIndexer(IndexSandboxManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        DirectorySandboxConfig config = manager.getConfig();
        SandboxRuntimeState state = manager.getRuntimeState();
        IndexWriter writer = manager.getWriter();

        if (writer == null) {
            state.setError("IndexWriter not available — index may be locked");
            return;
        }

        if (config.getSourcePath() == null) {
            state.setError("Source path not configured");
            return;
        }

        Path sourcePath = Paths.get(config.getSourcePath());
        if (!Files.isDirectory(sourcePath)) {
            state.setError("Source path not found: " + config.getSourcePath());
            return;
        }

        try {
            state.updateProgress(IndexStatus.SCANNING, 0, 0);
            state.addLogEntry(new LogEntry(LogEntry.Level.INFO, "Starting scan: " + sourcePath));

            Map<String, Long> existingDocs = readExistingDocs(writer);

            List<PathMatcher> matchers = buildMatchers(config.getFileInclusionFilter());
            List<Path> files = new ArrayList<Path>();
            Files.walk(sourcePath)
                .filter(Files::isRegularFile)
                .filter(p -> matchesFilter(p, matchers))
                .forEach(files::add);

            state.addLogEntry(new LogEntry(LogEntry.Level.INFO, "Found " + files.size() + " files to process"));

            int total = files.size();
            int processed = 0;
            int added = 0;
            int skipped = 0;

            for (Path file : files) {
                String id = file.toAbsolutePath().toString();
                long fileModified = Files.getLastModifiedTime(file).toMillis();
                Long indexedModified = existingDocs.remove(id);

                if (indexedModified != null && indexedModified >= fileModified) {
                    skipped++;
                } else {
                    if (indexedModified != null) {
                        writer.deleteDocuments(new Term(LuceneDocumentSchema.FIELD_ID, id));
                    }
                    indexFile(file, id, fileModified, writer);
                    state.addLogEntry(new LogEntry(LogEntry.Level.INFO, "Indexed: " + file.getFileName()));
                    added++;
                }

                processed++;
                int progress = (total == 0) ? 100 : (processed * 100 / total);
                state.updateProgress(IndexStatus.SCANNING, added, progress);
            }

            int removed = 0;
            for (String staleId : existingDocs.keySet()) {
                writer.deleteDocuments(new Term(LuceneDocumentSchema.FIELD_ID, staleId));
                state.addLogEntry(new LogEntry(LogEntry.Level.INFO,
                    "Removed stale: " + Paths.get(staleId).getFileName()));
                removed++;
            }

            writer.commit();
            state.setLastCommittedTimestamp(System.currentTimeMillis());

            int finalCount = 0;
            try (DirectoryReader reader = DirectoryReader.open(writer)) {
                finalCount = reader.numDocs();
            } catch (IOException ignored) {
            }

            state.updateProgress(IndexStatus.IDLE, finalCount, 100);
            state.addLogEntry(new LogEntry(LogEntry.Level.SUCCESS,
                "Commit complete. " + added + " added, " + skipped + " unchanged, "
                + removed + " removed. Total: " + finalCount));

        } catch (AlreadyClosedException e) {
            // Writer closed by JVM shutdown hook — silent exit
        } catch (IOException e) {
            state.setError("Indexing failed: " + e.getMessage());
            state.addLogEntry(new LogEntry(LogEntry.Level.ERROR, "Indexing failed: " + e.getMessage()));
        }
    }

    private Map<String, Long> readExistingDocs(IndexWriter writer) throws IOException {
        Map<String, Long> result = new HashMap<String, Long>();
        try (DirectoryReader reader = DirectoryReader.open(writer)) {
            HashSet<String> fields = new HashSet<String>();
            fields.add(LuceneDocumentSchema.FIELD_ID);
            fields.add(LuceneDocumentSchema.FIELD_LAST_MODIFIED_STORED);
            for (int i = 0; i < reader.maxDoc(); i++) {
                Document doc = reader.document(i, fields);
                String id = doc.get(LuceneDocumentSchema.FIELD_ID);
                String modStr = doc.get(LuceneDocumentSchema.FIELD_LAST_MODIFIED_STORED);
                if (id != null && modStr != null) {
                    result.put(id, Long.parseLong(modStr));
                }
            }
        } catch (IndexNotFoundException e) {
            // Fresh index — no existing documents
        }
        return result;
    }

    private void indexFile(Path file, String id, long lastModified, IndexWriter writer) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String content = new String(bytes, StandardCharsets.UTF_8);

        Document doc = new Document();
        doc.add(new StringField(LuceneDocumentSchema.FIELD_ID, id, Field.Store.YES));
        doc.add(new TextField(LuceneDocumentSchema.FIELD_TITLE, file.getFileName().toString(), Field.Store.YES));
        doc.add(new TextField(LuceneDocumentSchema.FIELD_CONTENTS, content, Field.Store.NO));
        doc.add(new LongPoint(LuceneDocumentSchema.FIELD_LAST_MODIFIED, lastModified));
        doc.add(new StoredField(LuceneDocumentSchema.FIELD_LAST_MODIFIED_STORED, String.valueOf(lastModified)));
        writer.addDocument(doc);
    }

    private List<PathMatcher> buildMatchers(String filter) {
        List<PathMatcher> matchers = new ArrayList<PathMatcher>();
        if (filter == null || filter.trim().isEmpty()) {
            return matchers;
        }
        FileSystem fs = FileSystems.getDefault();
        for (String glob : filter.split(",")) {
            glob = glob.trim();
            if (!glob.isEmpty()) {
                matchers.add(fs.getPathMatcher("glob:" + glob));
            }
        }
        return matchers;
    }

    private boolean matchesFilter(Path path, List<PathMatcher> matchers) {
        if (matchers.isEmpty()) {
            return true;
        }
        Path fileName = path.getFileName();
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(fileName)) {
                return true;
            }
        }
        return false;
    }
}
