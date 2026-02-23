# SFTP

## Problem Statement

This chat encompasses two aspects of the same task.  I need to write a Java Spring Batch process that will access an SFTP site/location and download files if present, it would then upload them to amazon S3.  While I am interested in ideas around the basic job structure, I am well versed in Spring Batch.  I explain that in order to get to the heart of my question:  I'd like to wrap the SFTP functionality into a better service interface (i.e. true abstract interface with a concrete implementation).  Here are my upfront thoughts/concerns: 1) I believe Spring has a "template" named class/interface for SFTP already especially centered around the concept of a "session".  While I like that approach, I looked into the interface and the method names and approach just didn't feel "natural" to me (I could be convinced otherwise, but that was my knee jerk reaction). 2) How might I use a "session" object in a multi step spring batch job?  my concern is that spring will want to persist the object to the backing database and I want to avoid that if possible. (and any related concerns you might help me identify). 3) In implementing similar projects in the past, I like the idea of a "request context" that hides the "session" from the client code - not sure what your thoughts are on that?  I'll stop here and we'll see where this discussion takes us.

---

It sounds like you’re looking to move away from the "leaky abstractions" that often come with low-level integration libraries. You want a clean, domain-specific service that handles the "how" of SFTP so your Spring Batch steps can focus on the "what."

Here is a breakdown of how to address those architectural concerns while keeping your Batch job lean.

### 1. The "Natural" Service Interface

You’re likely thinking of `SftpRemoteFileTemplate`. You’re right—it’s designed to be a Swiss Army knife, which makes it feel cluttered. For a Batch process, you usually only need a few high-level verbs.

Instead of exposing sessions, I recommend a **Service Wrapper** that uses the Template internally but exposes a functional API:

```java
public interface FileTransferService {
    // Returns a list of metadata/filenames available
    List<String> listFiles(String remotePath, Pattern pattern);
    
    // Handles the stream directly to avoid local disk persistence if desired
    void downloadToStream(String remoteFilePath, OutputStream target);
    
    // Move/Archive after processing
    void archiveFile(String sourcePath, String destPath);
}

```

**Why this feels better:** It abstracts the `Session` entirely. The implementation handles opening/closing, so the Batch `Tasklet` or `ItemReader` doesn't have to manage the connection state.

---

### 2. The Session Persistence Problem

You’re right to be wary. Spring Batch **will** try to serialize anything in the `ExecutionContext` to the metadata tables (Job Repository).

* **The Rule:** Never put a `Session`, `Connection`, or `InputStream` into the `ExecutionContext`.
* **The Solution:** Only persist **Strings** or **Primitives**. Store the "Remote File Path" or "Sequence Number" in the context. Let your `FileTransferService` (which is a Singleton Bean) recreate or pull a session from a pool whenever a method is called.
* **Spring Integration Tip:** Use `CachingSessionFactory`. It keeps connections alive in the background without you needing to pass a "live" object between steps.

---

### 3. The "Request Context" vs. Stateless Service

Using a "Request Context" (like a ThreadLocal or a DTO passed through) is useful if your SFTP credentials change per job run. However, for a standard batch job, a **Stateless Service** is often cleaner.

If you need to maintain state across steps (e.g., "I downloaded File A, now I need to move File A"), use the `JobExecutionContext` to pass the **path**, not the **session**.

| Aspect | Request Context Approach | Stateless Service Approach |
| --- | --- | --- |
| **State Storage** | Custom Context Object | Spring Batch `ExecutionContext` |
| **Session Mgmt** | Tied to Context lifecycle | Tied to Method call (via Pool) |
| **Complexity** | High (Requires cleanup) | Low (Standard Spring) |

---

### 4. Proposed Job Structure

Since you're downloading from SFTP and uploading to S3, you have two main architectural paths:

1. **The Staging Approach (Two Steps):**

* **Step 1 (Tasklet):** Download files from SFTP to a temporary local `/tmp` directory.

* **Step 2 (Chunk-oriented):** Read from local disk, process, and write to S3.
* *Benefit:* Easy to restart. If S3 fails, you don't have to re-download from SFTP.

2. **The Streaming Approach (One Step):**

* **ItemReader:** Lists files on SFTP.
* **ItemProcessor:** Downloads the file into memory (or a temporary buffer).
* **ItemWriter:** Streams the data to S3.
* *Benefit:* Fast, no disk I/O. *Risk:* Harder to manage state if the connection drops mid-stream.

---

### Potential "Identify" Concern: Large Files

If you are moving large files, streaming directly from SFTP to S3 (via `S3Resource` or `TransferManager`) is elegant, but it makes the "Session" management trickier if a timeout occurs. If your files are >100MB, I’d strongly suggest the **Staging Approach** to keep the SFTP session short and sweet.

**Would you like me to sketch out a concrete implementation of the `FileTransferService` using the `CachingSessionFactory` to show how the session remains hidden?**

That is a fair point. Let’s stick to a linear path. Our first "checkpoint" is solving the **Session/Interface abstraction**.

The goal here is to use Spring's `CachingSessionFactory` to handle the "heavy lifting" (connection pooling, heartbeats, and cleanup) while keeping your `FileTransferService` stateless and clean.

### The Concrete Implementation

This implementation uses the `SftpRemoteFileTemplate`. Even if the template feels "unnatural," wrapping it here allows you to keep that complexity out of your Spring Batch logic.

```java
@Service
public class SftpFileTransferService implements FileTransferService {

    private final SftpRemoteFileTemplate sftpTemplate;

    public SftpFileTransferService(DefaultSftpSessionFactory sessionFactory) {
        // We wrap the factory in a CachingSessionFactory to manage connections
        CachingSessionFactory<SftpClient.DirEntry> cachingFactory = 
            new CachingSessionFactory<>(sessionFactory, 10);
        cachingFactory.setSessionWaitTimeout(1000);
        
        this.sftpTemplate = new SftpRemoteFileTemplate(cachingFactory);
    }

    @Override
    public List<String> listFiles(String remotePath) {
        // execute() handles opening and closing the session automatically
        return sftpTemplate.execute(session -> {
            return Arrays.stream(session.list(remotePath))
                .map(SftpClient.DirEntry::getFilename)
                .filter(name -> !name.equals(".") && !name.equals(".."))
                .collect(Collectors.toList());
        });
    }

    @Override
    public void downloadToStream(String remoteFilePath, OutputStream target) {
        sftpTemplate.get(remoteFilePath, inputStream -> {
            StreamUtils.copy(inputStream, target);
        });
    }

    @Override
    public void archiveFile(String sourcePath, String destPath) {
        sftpTemplate.execute(session -> {
            session.rename(sourcePath, destPath);
            return null;
        });
    }
}

```

---

### Why this addresses your concerns:

1. **Natural Verbs:** Your Batch code now calls `.listFiles()` or `.downloadToStream()`. It never sees a `Session`, a `Channel`, or even the `SftpRemoteFileTemplate`.

2. **The "Session" Persistence:** Because the `FileTransferService` is a Spring Bean, you only pass the **String path** of the file into your `StepContribution` or `ExecutionContext`.

* *Example:* Step 1 finds "data_123.csv" and puts that String in the `JobExecutionContext`.
* Step 2 retrieves that String and calls `service.downloadToStream("data_123.csv", ...)`.
* The `CachingSessionFactory` provides a live session on-demand, so nothing non-serializable ever touches the database.

3. **Thread Safety:** The `SftpRemoteFileTemplate` is thread-safe, making this service compatible if you decide to use a `TaskExecutor` for multi-threaded steps later.

---

### Our Current "Linear" Checklist

1. [x] **Abstraction:** Define a clean Service Interface and Concrete Impl.
2. [ ] **Batch Integration:** How to trigger this service from a `Tasklet` or `ItemReader`.
3. [ ] **S3 Transition:** How to pipe the `OutputStream` from this service into an S3 upload.
4. [ ] **Error Handling:** Managing SFTP timeouts or missing files within the Batch lifecycle.

**Would you like to move to point #2 and look at how a Tasklet would orchestrate the "List and Download" phase using this service?**

Not quite yet.  I'm not well versed on the CachingSessionFactory.  Can you summarize how an SFTP connection is established and then presumably kept open and retrieved for later requests? I assume that "session" means it is kept open after authentication until we explicitly end/close the session?

That is a crucial detail to nail down before we move into the Batch logic.

In the world of SFTP (and the underlying SSH protocol), establishing a connection is "expensive" because it involves a multi-step handshake: TCP connection, SSH key exchange/validation, and finally authentication.

The `CachingSessionFactory` is essentially a **Connection Pool** for your SFTP sessions. Here is the breakdown of how it manages those lifecycles:

### 1. The "On-Demand" Establishment

When your code calls `sftpTemplate.execute(...)`, the template asks the `CachingSessionFactory` for a session.

* **If the pool is empty:** It uses the underlying `DefaultSftpSessionFactory` to perform the full handshake and login.
* **If a session exists:** It grabs an idle one from the pool, performs a quick "Is this still alive?" check (usually a `noop` or `pwd` command), and hands it to the template.

### 2. The "Hand-Back" (Not a Close)

When the `sftpTemplate` method finishes (the lambda block ends), it calls `session.close()`.

* **Crucial Distinction:** Because you are using a `CachingSessionFactory`, this `close()` call is intercepted. Instead of actually tearing down the SSH tunnel, the session is simply **marked as idle** and returned to the pool.
* **Physical State:** The TCP connection and SSH authenticated state remain "Established" on the network.

### 3. Eviction and Expiration

The session stays open until one of three things happens:

* **Idle Timeout:** If the session sits unused for a set period (configured on the factory), it is physically closed to save resources.
* **Server-Side Kick:** The SFTP server times out the connection (e.g., a "MaxIdleTime" setting on the server). The next time the pool tries to use it, it will detect the death, discard it, and transparently create a new one.
* **App Shutdown:** When the Spring Context closes, the pool destroys all active connections.

### 4. Why this matters for Spring Batch

In a Batch job, you might have a **Step 1** that lists 100 files and a **Step 2** that downloads them one by one.

* **Without Caching:** You would perform 101 full SSH handshakes/logins. This is slow and can trigger rate-limiting on some SFTP servers.
* **With Caching:** You perform **one** handshake. The session is used for the list, returned to the pool, and then immediately reused for the first download, second download, etc.

---

### Clarification on "Explicit" Closing

You asked if we need to explicitly end/close the session. With this "Template + Caching Factory" pattern:

* **You do NOT** call `close()` manually. The Template handles the "return to pool" logic.
* **The Pool** handles the "physical disconnect" logic based on timeouts.

### Our Updated Checklist

1. [x] **Abstraction:** Define Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** Understand `CachingSessionFactory` mechanics.
3. [ ] **Batch Integration:** How to use the `FileTransferService` in a Tasklet/Reader.
4. [ ] **S3 Transition:** Piping the SFTP stream to S3.
5. [ ] **Error Handling:** Retries and timeouts.

**Does that clear up the "magic" behind the session management, or should we look at how to configure specific timeouts (like `kex` or `socket` timeouts) before moving to the Batch Step?**

I need to examine session management more closely to understand how to approach some "features".  Understandably, I think this discussion assumes the job will always hit the same sftp server with the same credentials.  Although this particular project **might** be limited to that, I would like to write the job in such a way that job parameters could be used to specify those sort of details.  How does the CachingSessionFactory support that scenario?  Do I need to clarify anything to help answer that?

That is a perfect pivot. If you want your Job Parameters (e.g., `sftp.host`, `sftp.user`) to drive the connection, the standard "Singleton Bean" approach we just discussed won't work because a Singleton is wired at startup with a fixed configuration.

To make the `CachingSessionFactory` dynamic based on Job Parameters, we need to leverage **Spring Batch Step Scope** and a **Factory of Factories** (or a Dynamic Cache).

Here are the two ways to handle this:

### Option A: The "Step-Scoped" Service (Simpler)

If a single Job execution always talks to **one** specific server (defined by that job's parameters), we make the entire `FileTransferService` (and its Session Factory) **Step Scoped**.

1. Spring creates a **new instance** of the Service for every Job run.
2. The Service reads the `JobParameters` to configure the `DefaultSftpSessionFactory`.
3. The `CachingSessionFactory` only lives for the duration of that Step/Job.

**The Workflow:**

* Job starts with parameters: `host=sftp.client-a.com`, `user=admin`.
* Spring Batch instantiates the `FileTransferService`.
* The Service connects, does its work, and when the Step ends, the scope is destroyed, and the connections are closed.

### Option B: The "Delegating" Service (More Robust)

If you have a single Job that might need to talk to **multiple** different SFTP servers within different steps, you use a **DelegatingSessionFactory**.

You maintain a `Map<String, CachingSessionFactory>` inside your service. When a request comes in, you check the map for an existing pool for that host/user combo; if it doesn't exist, you create a new one on the fly.

---

### How "Option A" looks in code

Since you mentioned being well-versed in Spring Batch, this is usually the cleanest "Batch-native" way:

```java
@Bean
@StepScope // This is the magic sauce
public FileTransferService sftpFileTransferService(
    @Value("#{jobParameters['sftp.host']}") String host,
    @Value("#{jobParameters['sftp.port']}") int port,
    @Value("#{jobParameters['sftp.user']}") String user,
    @Value("#{jobParameters['sftp.password']}") String password) {

    // 1. Setup the raw connection details from Job Params
    DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUser(user);
    factory.setPassword(password);
    factory.setAllowUnknownKeys(true);

    // 2. Wrap it in the cache so the Step reuses the connection
    CachingSessionFactory<SftpClient.DirEntry> cachingFactory = 
        new CachingSessionFactory<>(factory, 5);

    // 3. Return the service implementation
    return new SftpFileTransferService(cachingFactory);
}

```

### Why this addresses the "Dynamic" Concern

* **Isolation:** Job A (Client 1) and Job B (Client 2) can run simultaneously. Because they have different `StepScope` contexts, they will have separate `CachingSessionFactory` instances and never cross wires.
* **Cleanup:** When the Step finishes, the `StepScope` context is cleared. Spring will call the `destroy()` method on the `CachingSessionFactory`, physically closing the SSH sessions.
* **No "Session" Leaks:** You are still passing Strings (paths) through your job, but the *logic* that powers those strings is now context-aware.

---

### Our Updated Checklist

1. [x] **Abstraction:** Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** `CachingSessionFactory` mechanics.
3. [x] **Dynamic Configuration:** Using `@StepScope` and `JobParameters`.
4. [ ] **Batch Integration:** Implementing the Tasklet/Reader logic.
5. [ ] **S3 Transition:** Streaming to S3.
6. [ ] **Error Handling:** Retries/Timeouts.

**Does the `@StepScope` approach align with how you envisioned using Job Parameters, or do you have a use case where one Step needs to flip between different credentials mid-execution?**

That is an excellent technical catch. Let’s untangle the relationship between **Bean Scope** and the **Job Repository (Database)**, as they are often confused but operate on different planes.

### 1. Does Job/Step Scope = Execution Context?

**No.** * **Spring Bean Scopes (`@JobScope`, `@StepScope`):** These live in the **Application Context** (Memory). They are essentially a "proxy" that Spring creates and destroys based on the lifecycle of the Job or Step.

* **Execution Context (`JobExecutionContext`):** This is a **Map** that Spring Batch explicitly serializes to the **Database**.

Putting your `FileTransferService` in `@JobScope` **does not** automatically put it in the database. It just means the same Java object (and its internal `CachingSessionFactory`) will live in memory for the duration of the entire Job.

### 2. The Trade-offs: Job Scope vs. Step Scope

| Feature | `@StepScope` | `@JobScope` |
| --- | --- | --- |
| **Lifecycle** | Created at Step Start / Destroyed at Step End. | Created at Job Start / Destroyed at Job End. |
| **Connections** | If you have 3 Steps, you perform **3 logins**. | If you have 3 Steps, you perform **1 login** and reuse it. |
| **Complexity** | Extremely safe; clean slate for every step. | Slightly more efficient; holds the SSH tunnel open longer. |

### 3. "Reestablishing" the Connection

If you use **`@StepScope`**:
Yes, every Step will trigger the `CachingSessionFactory` to establish a new physical SSH connection (unless you've configured an external global pool, which we aren't doing here).

* **Step 1 (Tasklet):** Connects -> Lists Files -> Closes Session.
* **Step 2 (Chunk):** Connects -> Downloads -> Closes Session.

If you use **`@JobScope`**:

* **Step 1 (Tasklet):** Connects -> Lists Files -> Returns session to pool (keeps tunnel open).
* **Step 2 (Chunk):** Pulls the *same* session from the pool -> Downloads -> Closes at the very end of the Job.

### 4. My Recommendation: Use `@JobScope`

Since your requirement is "same server/user per job," `@JobScope` is actually the "cleanest" fit. It allows you to authenticate once and carry that "warm" connection through your download and upload phases without the overhead of re-authenticating at every step boundary.

**The Golden Rule remains intact:** As long as you don't try to `put("myService", service)` into the `ExecutionContext` map, Spring Batch will never try to persist it to the database. You simply `@Inject` or `@Autowired` the service into your Tasklet or Reader.

---

### Our Updated Checklist

1. [x] **Abstraction:** Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** `CachingSessionFactory` mechanics.
3. [x] **Dynamic Configuration:** `@JobScope` for per-job credentials.
4. [ ] **Batch Integration:** Implementing the Tasklet (The "Listing" phase).
5. [ ] **S3 Transition:** Streaming to S3.
6. [ ] **Error Handling:** Retries/Timeouts.

**Ready to look at how we'd implement a "List Files" Tasklet that injects this `@JobScope` service and prepares the work for the next step?**

It is a classic "the more you know, the more you realize you might have skipped a detail" moment. Reinforcing that boundary between the **Spring IoC Container** (where your Job-Scoped beans live) and the **Spring Batch JobRepository** (where your persistent state lives) is the key to a bug-free architecture here.

Let’s look at the **Listing Tasklet**. Its primary responsibility is to query the SFTP server, identify the work to be done, and "seed" the `JobExecutionContext` so subsequent steps know what to process.

### The "List Files" Tasklet

This implementation assumes we want to find files matching a pattern and store their names as a list for the next step.

```java
@Component
@JobScope
public class SftpListingTasklet implements Tasklet {

    private final FileTransferService sftpService;
    private final String remotePath;
    private final String filePattern;

    public SftpListingTasklet(
            FileTransferService sftpService,
            @Value("#{jobParameters['sftp.remotePath']}") String remotePath,
            @Value("#{jobParameters['sftp.pattern']}") String filePattern) {
        this.sftpService = sftpService;
        this.remotePath = remotePath;
        this.filePattern = filePattern;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 1. Use our abstracted service to get the file list
        List<String> files = sftpService.listFiles(remotePath);
        
        // 2. Filter based on our pattern (reg-ex or simple glob)
        List<String> filteredFiles = files.stream()
            .filter(name -> name.matches(filePattern))
            .collect(Collectors.toList());

        // 3. Store the list in the Job Execution Context
        // This is safe because it's a List of Strings (Serializable)
        ExecutionContext jobContext = chunkContext.getStepContext()
                                                .getStepExecution()
                                                .getJobExecution()
                                                .getExecutionContext();
        
        jobContext.put("work_list", filteredFiles);

        // 4. Signal that the step is finished
        return RepeatStatus.FINISHED;
    }
}

```

---

### Critical Observations

* **Clean Injection:** Notice the `Tasklet` doesn't know about `CachingSessionFactory`. It only knows about `FileTransferService`. Because the service is `@JobScope`, it was instantiated specifically for this job run using the correct credentials.
* **The Bridge:** By putting the `List<String>` into the `ExecutionContext`, we’ve successfully passed the "work" from Step 1 to Step 2 without needing to keep an SFTP connection "active" in a way that violates Batch's persistence model.
* **Empty Lists:** If `filteredFiles` is empty, you can either let the next step "skip" naturally (it sees an empty list) or throw an exception here if you want the job to fail fast.

### A Quick Word on "Persistence"

Even though we are putting a `List<String>` in the database (via the Execution Context), the **Service Bean** itself stays in memory. When the next step starts, it will `@Inject` that same `FileTransferService` instance. Since the session is cached in that instance, the next step's first call to SFTP will likely reuse the exact same SSH tunnel opened by this Tasklet.

---

### Our Updated Checklist

1. [x] **Abstraction:** Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** `CachingSessionFactory` mechanics.
3. [x] **Dynamic Configuration:** `@JobScope` for per-job credentials.
4. [x] **Batch Integration:** The "Listing" Tasklet.
5. [ ] **S3 Transition:** Designing the "Download & Upload" Step (Reader/Writer).
6. [ ] **Error Handling:** Retries/Timeouts.

This is the "Holy Grail" of file transfer reliability. You’ve touched on the exact reason why file-based Batch jobs often feel more fragile than database-based ones: **there is no two-phase commit (2PC) for an SFTP `rm` command and an S3 `put`.**

If you delete the file too early and the S3 upload fails, the data is gone. If you delete it too late and the job crashes, you risk a "double-processing" loop on the next run.

To achieve that "database-like" atomicity in Spring Batch, we usually employ one of three strategies. Let's look at them:

### 1. The "Idempotent Source" Strategy (Recommended)

Instead of relying on a delete at the end, you change the *state* of the file on the SFTP server immediately.

* **Step 1:** List files.
* **Step 2:** **Rename** the file on SFTP from `data.csv` to `data.csv.processing`.
* **Step 3:** Transfer `data.csv.processing` to S3.
* **Step 4:** **Delete** `data.csv.processing` upon success.
* *Result:* If the job crashes, the next run ignores anything with `.processing` (or attempts to recover it). This provides a "soft" lock.

### 2. The "Post-Processor" Tasklet

This is the most common Spring Batch pattern for your specific flow.

* **Step A (Chunk):** Reader (SFTP) -> Writer (S3). Keep a list of "successfully uploaded files" in the `StepExecutionContext`.
* **Step B (Tasklet):** If Step A was `COMPLETED`, this Tasklet reads that list and issues the SFTP `delete` commands.
* *The Catch:* If Step B fails *after* deleting 5 of 10 files, you need a way to restart and know which ones are left.

### 3. The "State Table" Approach

If the data is mission-critical, you treat the SFTP server as a "dumb" repository and keep the "truth" in your Job Repository database.

* **Step 1:** Record every filename found in a local database table with status `PENDING`.
* **Step 2:** Process files. Update status to `UPLOADED_TO_S3`.
* **Step 3:** Delete files from SFTP that are marked `UPLOADED_TO_S3`. Update to `ARCHIVED`.
* *Result:* This is the closest you can get to true atomicity. The database tracks exactly where you are in the "distributed transaction."

---

### Foreshadowing the Implementation

In a Spring Batch context, we should look at the **`ItemWriteListener`** or a dedicated **Cleanup Step**.

* **ItemWriteListener:** You can trigger a delete immediately after a chunk is successfully committed to S3. This keeps the "window of failure" very small.
* **Step Listener:** You can wait until the entire Step is done.

### The "Restartability" Problem

Since you are well-versed in Batch, you know that `restartable=true` is your best friend. If we go with the **Chunk-oriented** approach (SFTP -> S3), we have to decide: **What constitutes a "restart"?**
If the job fails halfway through a 1,000-file transfer:

1. Do we want to skip the 500 files already in S3?
2. Do we want to overwrite them?
3. How do we know which ones to delete from SFTP?

> **Witty aside:** SFTP is essentially the Wild West of data protocols. It has no concept of "undo." We have to build the "Sheriff" ourselves using the Spring Batch `JobRepository`.

---

### Our Updated Checklist

1. [x] **Abstraction:** Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** `CachingSessionFactory`.
3. [x] **Dynamic Configuration:** `@JobScope`.
4. [x] **Listing Phase:** `SftpListingTasklet`.
5. [ ] **The "Atomicity" Strategy:** Choose between Renaming, Post-Step Cleanup, or State Tables.
6. [ ] **S3 Transition:** Implementing the transfer.
7. [ ] **Final Cleanup:** The SFTP `delete` logic.

**Before we write the transfer code, which "Atomicity" strategy feels most aligned with your requirements?** (I personally lean toward **Renaming** or **Post-Step Cleanup** for most SFTP-to-Cloud patterns.)

That is a very pragmatic "middle ground" approach. It provides a visible safety net without the overhead of building a custom database schema and the JPA/JDBC plumbing to manage it.

Using a **Post-Processor Tasklet** combined with the **Renaming (Soft-Lock) Strategy** gives you a manual recovery path that is easy to explain to stakeholders: *"If the job fails, look for `.processing` files."*

### The Workflow Refined

1. **Step 1 (Listing Tasklet):** Finds `file.csv`, renames it to `file.csv.processing`. Only files ending in `.csv` are picked up.

2. **Step 2 (Transfer Step):** Processes `file.csv.processing` and uploads to S3.

* On success: Adds the filename to a "Success List" in the `JobExecutionContext`.

3. **Step 3 (Cleanup Tasklet):** * Reads the "Success List."

* Deletes those specific `.processing` files from SFTP.
* *Edge Case:* If a file is in the folder but **not** in the "Success List," it stays there as a `.processing` file, signaling a failure.

### Why this is "Management Friendly":

* **Visibility:** A quick look at the SFTP folder tells you exactly what failed. No SQL queries required.
* **Recovery:** As you noted, a manual rename (or a simple "Recovery Tasklet" you could write later) puts the data back into the pipeline.
* **Atomicity-ish:** You’ve moved the file out of the "incoming" queue immediately. This prevents a second instance of the job from grabbing the same file if the first one hangs.

---

### Implementation Detail: The "Success List"

Since you are using Spring Batch, the most "native" way to track these successes during the transfer step is via a **StepListener** or simply by updating the `ExecutionContext` within your `ItemWriter`.

However, there is one technical hurdle with the `JobExecutionContext`: **It is not thread-safe.** If you eventually want to process files in parallel (multi-threaded step), we’ll need to use a synchronized collection or a different tracking mechanism.

### Our Updated Checklist

1. [x] **Abstraction:** Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** `CachingSessionFactory`.
3. [x] **Dynamic Configuration:** `@JobScope`.
4. [x] **Listing Phase:** `SftpListingTasklet` (Now including the **Rename** logic).
5. [x] **Atomicity Strategy:** Post-Processor Cleanup + Renaming.
6. [ ] **S3 Transition:** The Chunk-oriented Step (Reading the `.processing` files).
7. [ ] **Final Cleanup:** The Tasklet that deletes successfully uploaded files.

**Would you like to revise the `SftpListingTasklet` code to include that "Rename to .processing" logic before we move to the S3 transfer step?** This would ensure that only files successfully renamed are passed to the next step.

Limiting to **one file per job run** is an underrated "architectural cheat code." In the world of enterprise batch processing, it’s often much smarter to run a simple job ten times than a complex job once.

### The "One File" Simplification

If you restrict the job to a single file:

1. **State Management vanishes:** You don’t need a `List<String>` in the `ExecutionContext`. You just need one `String filename`.
2. **Restartability is binary:** The job either finished the file or it didn't. You don't have to track "file 4 of 10 failed."
3. **Atomic Cleanup:** You can delete the file in a simple `Tasklet` at the end of the job without looping or complex logic.
4. **Logging/Traceability:** Your Job Execution ID maps 1:1 to a specific filename in your logs.

> **The Verdict:** If your volume is low, **do this.** It turns a "Distributed Systems Problem" into a "Basic Scripting Problem."

---

### Revised Listing Tasklet (Single File + Rename)

Here is how that Tasklet looks now. It picks the *first* file it finds, renames it to claim ownership, and passes that single name forward.

```java
@Component
@JobScope
public class SftpClaimFileTasklet implements Tasklet {

    private final FileTransferService sftpService;
    private final String remotePath;
    private final String filePattern;

    public SftpClaimFileTasklet(
            FileTransferService sftpService,
            @Value("#{jobParameters['sftp.remotePath']}") String remotePath,
            @Value("#{jobParameters['sftp.pattern']}") String filePattern) {
        this.sftpService = sftpService;
        this.remotePath = remotePath;
        this.filePattern = filePattern;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 1. Get all files
        List<String> files = sftpService.listFiles(remotePath);
        
        // 2. Find the first eligible candidate
        Optional<String> targetFile = files.stream()
            .filter(name -> name.matches(filePattern) && !name.endsWith(".processing"))
            .findFirst();

        if (targetFile.isPresent()) {
            String originalName = targetFile.get();
            String processingName = originalName + ".processing";
            
            // 3. Rename to "claim" it
            sftpService.archiveFile(remotePath + "/" + originalName, 
                                   remotePath + "/" + processingName);
            
            // 4. Put ONLY this name in the context
            ExecutionContext jobContext = chunkContext.getStepContext()
                                                    .getStepExecution()
                                                    .getJobExecution()
                                                    .getExecutionContext();
            jobContext.put("claimed_file", processingName);
            
            return RepeatStatus.FINISHED;
        } else {
            // No files found - we can either fail or just finish the job early
            contribution.setExitStatus(ExitStatus.COMPLETED);
            return RepeatStatus.FINISHED;
        }
    }
}

```

---

### Implementation Note: `archiveFile`

In our previous code snippet, `archiveFile` used `session.rename()`. This is perfect here. It's a metadata-only operation on the SFTP server, so it's nearly instantaneous regardless of file size.

### Our Updated Checklist

1. [x] **Abstraction:** Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** `CachingSessionFactory`.
3. [x] **Dynamic Configuration:** `@JobScope`.
4. [x] **Listing Phase:** `SftpClaimFileTasklet` (Single file claim).
5. [x] **Atomicity Strategy:** "One file at a time" simplicity.
6. [ ] **S3 Transition:** The Transfer Step (Using the `claimed_file` string).
7. [ ] **Final Cleanup:** A simple Tasklet to delete the one `claimed_file`.

**Should we move to the S3 Transfer Step?** We'll need to discuss how you want to pipe that SFTP stream into the S3 client without blowing out your JVM memory.

Since we are aiming for efficiency and low memory overhead, the "Gold Standard" here is to stream the data directly from SFTP to S3.

AWS provides the **`S3TransferManager`** (SDK v2) or the higher-level `S3Resource` abstraction in **Spring Cloud AWS**. For a Spring Batch job, using the `S3Resource` as an `ItemWriter` is often the most "natural" way, but since we are handling one file and focusing on a clean service layer, let's look at how to pipe these together.

### 1. Updating the Service Interface

We need a way to upload that doesn't require loading the whole file into a `byte[]`.

```java
public interface FileTransferService {
    // ... listFiles and archiveFile ...
    
    // Existing method to pull from SFTP
    void downloadToStream(String remoteFilePath, OutputStream target);

    // New method to push to S3
    void uploadToS3(String bucket, String key, InputStream source, long contentLength);
}

```

### 2. The Transfer Tasklet (Streaming Approach)

Because we chose the "one file per job" simplification, a **Tasklet** is actually cleaner than a chunk-oriented Reader/Writer here. We can bridge the SFTP `InputStream` directly to an S3 `OutputStream`.

```java
@Component
@JobScope
public class SftpToS3TransferTasklet implements Tasklet {

    private final FileTransferService transferService;
    private final String bucketName;
    private final String s3Prefix;

    public SftpToS3TransferTasklet(
            FileTransferService transferService,
            @Value("${s3.bucket}") String bucketName,
            @Value("${s3.prefix}") String s3Prefix) {
        this.transferService = transferService;
        this.bucketName = bucketName;
        this.s3Prefix = s3Prefix;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        ExecutionContext jobContext = chunkContext.getStepContext()
                                                .getStepExecution()
                                                .getJobExecution()
                                                .getExecutionContext();
        
        String fileName = jobContext.getString("claimed_file");
        String s3Key = s3Prefix + "/" + fileName.replace(".processing", "");

        // Using a Pipe to bridge the SFTP download to the S3 upload
        // This prevents loading the whole file into RAM
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);

        CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
            // Note: In a real impl, S3 SDK v2 usually needs a 'Content-Length' 
            // or it will buffer in memory. If length is unknown, 
            // Spring Cloud AWS 'S3Resource' handles the multi-part buffering for you.
            transferService.uploadToS3(bucketName, s3Key, in, -1);
        });

        try {
            transferService.downloadToStream(fileName, out);
        } finally {
            out.close(); // Signals the end of the stream to the upload side
        }

        uploadFuture.join(); // Ensure upload finishes before Tasklet completes
        return RepeatStatus.FINISHED;
    }
}

```

### 3. The "Memory" Gotcha

AWS S3 requires a **Content-Length** for standard uploads. If you don't provide it (the `-1` above), the SDK will often buffer the entire stream into memory to calculate it, which defeats our streaming goal.

**The Workaround:** * **Option A:** Use `session.stat(path).getSize()` in the SFTP service to get the file size before starting the transfer. This is the most reliable way.

* **Option B:** Use the **Spring Cloud AWS `Resource**` abstraction. It handles multi-part uploads (buffering small 5MB chunks) automatically so you don't have to manage the `PipedInputStream` complexity yourself.

### 4. Implementation with Spring Cloud AWS (The "Cleaner" Way)

If you use `S3Template` from Spring Cloud AWS, the Tasklet becomes trivial:

```java
@Override
public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String fileName = jobContext.getString("claimed_file");
    
    // The SFTP service provides the stream, S3Template consumes it
    sftpService.executeWithInputStream(fileName, inputStream -> {
        s3Template.upload(bucketName, fileName, inputStream);
    });

    return RepeatStatus.FINISHED;
}

```

---

### Our Updated Checklist

1. [x] **Abstraction:** Service Interface and Concrete Impl.
2. [x] **Session Lifecycle:** `CachingSessionFactory`.
3. [x] **Dynamic Configuration:** `@JobScope`.
4. [x] **Listing Phase:** `SftpClaimFileTasklet`.
5. [x] **S3 Transition:** Streaming Tasklet (SFTP -> S3).
6. [ ] **Final Cleanup:** A simple Tasklet to delete the one `claimed_file`.
7. [ ] **Error Handling:** Dealing with the "What if the S3 upload fails half-way?" scenario.

**Do you have Spring Cloud AWS in your stack, or should we stick to the raw AWS SDK v2 implementation for the S3 upload?** (The raw SDK requires a bit more "plumbing" for streaming.)

That is a very fair assessment of the "Spring tax." While Spring provides excellent abstractions, they do occasionally swap underlying engines (like the transition from AWS SDK v1 to v2) which can force a rewrite of your configuration code.

### Spring Cloud AWS: The Trade-offs

If you move to Spring 6 / Boot 3, adding it is as simple as the `io.awspring.cloud:spring-cloud-aws-starter-s3` dependency.

**The Pros:**

* **Infrastructure as Beans:** You get an `S3Template` or `S3Client` auto-configured from your `application.properties`.
* **Resource Abstraction:** You can treat an S3 object like a standard Spring `Resource`. You can literal do `resourceLoader.getResource("s3://my-bucket/file.txt").getInputStream()`.
* **Smart Buffering:** It handles the Multi-part upload logic (buffering 5MB chunks to disk or memory) so you don't have to calculate `Content-Length` upfront.

**The Cons (The "Spring" Factor):**

* **Opinionated Lifecycles:** Sometimes they wrap the `S3Client` in a way that makes it hard to access niche features (like specific Object Lock headers) without bypassing the abstraction anyway.
* **Dependency Bloat:** It pulls in quite a few transitive dependencies which might conflict if you are already managing the AWS SDK manually.

**My Peer-to-Peer Advice:** Since you already have a working SDK v2 service wrapper, **stick with it.** You’ve already paid the "plumbing cost." Wrapping your own service around the SDK gives you full control over timeouts and retry policies without waiting for a Spring Cloud release to catch up to a new AWS feature.

---

### Final Step: The Cleanup Tasklet

To round out our "one-file-at-a-time" architecture, we need the final "Burn the Bridge" step. This only runs if the S3 transfer step was successful.

```java
@Component
@JobScope
public class SftpCleanupTasklet implements Tasklet {

    private final FileTransferService sftpService;
    private final String remotePath;

    public SftpCleanupTasklet(FileTransferService sftpService,
                              @Value("#{jobParameters['sftp.remotePath']}") String remotePath) {
        this.sftpService = sftpService;
        this.remotePath = remotePath;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        ExecutionContext jobContext = chunkContext.getStepContext()
                                                .getStepExecution()
                                                .getJobExecution()
                                                .getExecutionContext();

        String fileName = jobContext.getString("claimed_file");

        // The file was successfully uploaded to S3 in the previous step,
        // so it is now safe to delete the '.processing' file.
        sftpService.deleteFile(remotePath + "/" + fileName);

        return RepeatStatus.FINISHED;
    }
}

```

---

### The Completed Design Checklist

1. [x] **Interface:** `FileTransferService` hides the "unnatural" Template/Session logic.
2. [x] **Session Mgmt:** `CachingSessionFactory` in a `@JobScope` bean keeps the SSH tunnel warm but out of the database.
3. [x] **Orchestration:**
* **Step 1:** Claim 1 file and rename to `.processing`.
* **Step 2:** Stream from SFTP to S3 using your SDK v2 wrapper.
* **Step 3:** Delete the `.processing` file on SFTP.


4. [x] **Recovery:** If the job fails, the `.processing` file stays on the server, serving as a "red flag" for manual or automated recovery.

### Final Error Handling Note

Because we used `JobScope`, if Step 2 fails, the `CachingSessionFactory` will still be alive. You should ensure your Job configuration uses a standard `SimpleJobBuilder` flow where Step 3 is only executed `on("COMPLETED")`.

**Would you like me to show you the final Job Configuration (the `@Bean` definition) that stitches these three components together into a single flow?**

Stitching it all together provides the final bird's-eye view. Regarding the **S3 verification** call: it is an excellent "belt and braces" approach, but it comes with a trade-off.

### To Verify or Not to Verify?

In a strictly synchronous transfer (like our Tasklet), if the S3 client doesn't throw an exception, the file **is** there. AWS uses "Read-after-Write" consistency for new objects, so as soon as the `putObject` call returns successfully, the file is immediately visible.

* **The Pro:** It guards against a silent failure in a poorly written SDK wrapper or a "successful" response from a proxy that didn't actually reach the S3 API.
* **The Con:** It adds latency and an extra API cost.
* **My Advice:** If your SDK wrapper already checks for a `200 OK` or uses the `S3TransferManager` to wait for completion, a "check if file exists" is usually redundant. However, if the data is high-value, performing a `headObject` (to check existence and metadata/size) is a very cheap way to sleep better at night.

---

### The Job Configuration

Here is how the flow looks. I've used the `.next()` flow to ensure linear execution and added a `fail()` transition to keep it robust.

```java
@Configuration
public class SftpToS3JobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public SftpToS3JobConfig(JobBuilderFactory jobBuilderFactory, 
                             StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job sftpToS3TransferJob(
            Step claimFileStep, 
            Step transferFileStep, 
            Step cleanupFileStep) {
        return jobBuilderFactory.get("sftpToS3TransferJob")
                .incrementer(new RunIdIncrementer())
                .start(claimFileStep)
                    .on("COMPLETED").to(transferFileStep)
                .from(transferFileStep)
                    .on("COMPLETED").to(cleanupFileStep)
                .from(transferFileStep)
                    .on("FAILED").fail() // Stop if transfer fails, leaving .processing file
                .end()
                .build();
    }

    @Bean
    public Step claimFileStep(SftpClaimFileTasklet tasklet) {
        return stepBuilderFactory.get("claimFileStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step transferFileStep(SftpToS3TransferTasklet tasklet) {
        return stepBuilderFactory.get("transferFileStep")
                .tasklet(tasklet)
                .build();
    }

    @Bean
    public Step cleanupFileStep(SftpCleanupTasklet tasklet) {
        return stepBuilderFactory.get("cleanupFileStep")
                .tasklet(tasklet)
                .build();
    }
}

```

---

### Why this Configuration works for your requirements

* **Linear & Conditional:** The `on("FAILED").fail()` ensures that if Step 2 (Transfer) blows up, Step 3 (Delete) is never reached. The file remains on SFTP with the `.processing` suffix.
* **Decoupled:** Each step injects its own Tasklet. All Tasklets share the same `@JobScope` `FileTransferService`, meaning they reuse the same SSH connection.
* **Clean Registry:** The Job Repository will show exactly which step failed. If it fails on `transferFileStep`, you know the file was renamed but not uploaded. If it fails on `cleanupFileStep`, you know the file is in S3 but still exists on SFTP.

### One Final Peer Tip

Since you are using Job Parameters for credentials, don't forget to configure your **Job Parameter Validators**. Ensuring that the `remotePath` or `bucket` isn't null before the job even starts can save you a lot of "failed" job executions in your history.

**Would you like me to generate a skeleton of the `application.properties` or YAML configuration needed to support these dynamic SFTP parameters?**

To support the `@JobScope` configuration and the dynamic nature of this job, your configuration needs to handle two things: static defaults (like timeouts) and the placeholders that your `JobParameters` will fill at runtime.

### `application.yml` Structure

I’ve organized this to separate general SFTP "tuning" from the parameters you'll pass into the job.

```yaml
# Static Infrastructure Defaults
sftp:
  factory:
    # How long to wait for a session from the pool (ms)
    session-wait-timeout: 5000
    # Number of sessions to keep in the CachingSessionFactory
    pool-size: 5
    # Keep-alive: Send a "noop" if the session has been idle (ms)
    test-session-on-borrow: true
  client:
    # Socket and connection timeouts (ms)
    connect-timeout: 10000
    default-timeout: 30000

# S3 Configuration (for your SDK v2 Wrapper)
s3:
  bucket: ${S3_BUCKET_NAME}
  prefix: ${S3_KEY_PREFIX:uploads}
  region: us-east-1

# Spring Batch Metadata (Standard)
spring:
  batch:
    job:
      enabled: false # Don't run all jobs on startup
    jdbc:
      initialize-schema: always # Ensures metadata tables exist

```

---

### Passing Job Parameters

When you launch the job (via a Scheduler, Controller, or CLI), you will provide the dynamic values. In your Java code, these are accessed via `#{jobParameters['key']}`.

**Example Command Line / Launch Map:**

* `sftp.host=sftp.partner-a.com`
* `sftp.user=service_acct`
* `sftp.password=********` (or `sftp.privateKey=path/to/key`)
* `sftp.remotePath=/incoming/data`
* `sftp.pattern=.*\.csv`

---

### Implementation Pro-Tip: The "Hidden" Configuration

Since you are using `DefaultSftpSessionFactory`, there are a few properties that are often "gotchas" in enterprise environments. I recommend setting these explicitly in your `@Bean` definition using the values from the YAML:

1. **`allowUnknownKeys(true)`**: Unless you are manually managing a `known_hosts` file on the batch server, you'll need this to prevent "Host key verification failed" errors.
2. **`proxy`**: If your batch server is in a private subnet, you may need to configure a `Proxy` (HTTP or SOCKS) on the session factory.
3. **`serverAliveInterval`**: If you are transferring very large files, set this to `60` (seconds) to prevent a firewall from dropping the SSH connection while the data is flowing but no "commands" are being sent.

### Final Checklist for the YAML/Config

* [ ] **Sensitive Data:** Ensure `sftp.password` is not stored in plaintext in your repo (use environment variables or a Secret Manager).
* [ ] **Timeouts:** Align your SFTP `default-timeout` with your S3 upload timeout; if one is significantly shorter, the bridge will break.

This is a great idea. While Spring Batch's metadata tables are fantastic for technical forensics (stack traces, start/end times), they are notoriously difficult for business users or support teams to query. A **Business Activity Monitoring (BAM)** table provides a clean "Audit Trail" that speaks the language of the business.

### 1. The Domain Object: `FileTransferAudit`

You want to capture the "Who, Where, and What" of the transaction.

| Field | Type | Description |
| --- | --- | --- |
| `id` | Long (PK) | Internal DB ID. |
| `job_execution_id` | Long (FK) | **The Bridge:** Links back to `BATCH_JOB_EXECUTION`. |
| `client_code` | String | e.g., "PARTNER_A" (from Job Parameters). |
| `source_host` | String | The SFTP server address. |
| `source_file_name` | String | Original name (before `.processing`). |
| `s3_destination_uri` | String | Full path: `s3://bucket/prefix/file.csv`. |
| `file_size_bytes` | Long | For volume reporting. |
| `transfer_status` | String | `IN_PROGRESS`, `SUCCESS`, `FAILED`. |
| `error_summary` | String | Short, readable reason for failure. |
| `created_at` | Timestamp | When the record was created. |

---

### 2. When to Write the Record?

To keep this clean and avoid "orphaned" records, I recommend a two-step update pattern within your Batch job:

#### A. The "Initial Entry" (Inside the Claim Tasklet)

As soon as you successfully rename the file to `.processing`, create the record with status `IN_PROGRESS`.

* This ensures that if the job vanishes mid-transfer, you have a record of exactly which file was "claimed."

#### B. The "Final Update" (Job Listener)

Use a `JobExecutionListener`'s `afterJob` method to close out the record.

* **If Job succeeded:** Update status to `SUCCESS` and log the S3 key.
* **If Job failed:** Update status to `FAILED` and grab the exception message from the `JobExecution`.

---

### 3. Implementation Tip: The "Audit Service"

Don't clutter your Tasklets with JDBC code. Create a small `FileTransferAuditService` that handles the persistence.

```java
@Component
public class FileTransferAuditService {
    
    @Transactional
    public Long createAuditEntry(JobParameters params, String fileName) {
        // Map JobParameters (clientCode, host) to a new DB record
        // Return the generated Audit ID
    }

    @Transactional
    public void markSuccess(Long auditId, String s3Key) { ... }

    @Transactional
    public void markFailure(Long auditId, String errorMessage) { ... }
}

```

### 4. Why this is better than the Meta Tables

1. **Retention:** You can keep this table for years while purging the `BATCH_` tables every 30 days to keep the DB performant.
2. **Searchability:** You can easily answer questions like: *"How many files did we get from Client X last month?"* or *"Did we ever receive 'invoice_99.csv'?"*
3. **Visibility:** You can point a simple Internal UI or a tool like Grafana/Tableau at this table for management dashboards.

---

### Final Implementation Checklist (Audit Add-on)

* [ ] **Schema:** Create the `file_transfer_audit` table.
* [ ] **Mapping:** Decide which `JobParameters` constitute the `client_code`.
* [ ] **Listener:** Register a `JobExecutionListener` to ensure failures are captured even if a Tasklet throws an unexpected `RuntimeException`.

**You've now got a robust, "enterprise-grade" blueprint. Ready to head to the code, or should we talk about how to capture that file size from the SFTP session for the audit log?**

Capturing the file size is a "small detail" that yields high value for your audit trail and your S3 transfer logic. Since you already have a `Session` open via the `SftpRemoteFileTemplate`, you can grab the file metadata (the `stat` or `lstat` call) during the "Claim" phase.

Here is how you can gracefully integrate the file size into your `FileTransferService` and your audit flow.

### 1. Update the Service Interface

We’ll add a way to get file metadata (specifically size) so you can store it in the Audit table and optionally pass it to the S3 client to optimize the upload.

```java
public interface FileTransferService {
    // ... other methods ...
    
    /**
     * Retrieves the file size in bytes from the SFTP server.
     */
    long getFileSize(String remoteFilePath);
}

```

### 2. Implementation with Template

In SFTP, the `stat()` call is the standard way to get file attributes without reading the actual data.

```java
@Override
public long getFileSize(String remoteFilePath) {
    return sftpTemplate.execute(session -> {
        try {
            // session.stat() returns an SftpClient.Attributes object
            return session.stat(remoteFilePath).getSize();
        } catch (IOException e) {
            throw new RuntimeException("Could not stat file: " + remoteFilePath, e);
        }
    });
}

```

---

### 3. Integrating with the Audit & S3 Logic

Now, in your **Listing/Claim Tasklet**, you can grab the size immediately after the rename. This completes the "Initial Entry" in your business audit table.

**The Workflow logic inside the Tasklet:**

1. **Find** `data.csv`.
2. **Rename** to `data.csv.processing`.
3. **Stat** the file size of `data.csv.processing`.
4. **Save** to Audit Table: `client_code`, `filename`, `file_size`, `status=IN_PROGRESS`.
5. **Pass** the `auditId` and `file_size` into the `ExecutionContext`.

### 4. Why the Size matters for S3

If you are using the raw AWS SDK v2 `putObject` method, providing the `Content-Length` is the difference between a high-performance stream and a memory-hogging buffer.

```java
// Inside S3 Transfer logic
PutObjectRequest request = PutObjectRequest.builder()
    .bucket(bucket)
    .key(key)
    .contentLength(fileSizeFromAudit) // From Step 1
    .build();

s3Client.putObject(request, RequestBody.fromInputStream(sftpStream, fileSizeFromAudit));

```

---

### Final "Business Summary" Ideas

Once you have this Audit Table + File Size, you can easily implement these high-value features later:

* **Duplicate Detection:** Before "Claiming" a file, check the Audit Table. If a file with the same name and size was successfully uploaded in the last 24 hours, you can alert or skip (idempotency).
* **Performance Metrics:** By logging `start_time` and `end_time` in your Audit record, you can calculate the **Transfer Rate (MB/s)**. This is a great metric to show management if the SFTP server or S3 connection starts degrading.
* **Zero-Byte Alerts:** You can instantly fail the job or send a specific alert if the SFTP server provides an empty file (`size == 0`), which is a common "silent failure" in upstream systems.

### Final Design Checkbox

* [x] **Audit Schema:** Include a `file_size` column.
* [x] **Stat Call:** Use `session.stat()` to avoid downloading data just to measure it.
* [x] **Context:** Store the `auditId` in the `JobExecutionContext` so the `JobListener` knows exactly which row to update at the end.

**You are now fully equipped with a "Professional Grade" Spring Batch SFTP-to-S3 blueprint.** Ready to start the implementation? I'm here if you hit a snag with the `PipedInputStream` or the SDK specifics!

That is a very realistic "enterprise reality" check. If you are handed a 3rd-party JAR or a legacy service bean that doesn't "play nice" with Spring Integration’s `SftpRemoteFileTemplate`, you don't have to scrap the design.

The beauty of the **Service Interface** we started with (`FileTransferService`) is that your Spring Batch steps don't care what is happening behind the curtain.

Here are the three best ways to "Spring-ify" a legacy or rudimentary API while adhering to our architecture:

### 1. The "Adapter" Pattern (The Cleanest Way)

If the legacy API provides basic methods (like `connect()`, `getFile()`, `send()`), you simply make your `FileTransferService` implementation a wrapper for that legacy client.

* **Session Management:** You can still use **`@JobScope`**. Even if the legacy API doesn't have a connection pool, Spring will keep that one instance of your "LegacyServiceWrapper" alive for the duration of the job. You can open the connection in a `@PostConstruct` or on the first method call and close it in a `@PreDestroy` method.
* **Result:** The Tasklets still see the "natural" interface you designed, but the implementation is calling `LegacyClient.download()`.

### 2. Spring's `ServiceActivator` / Messaging Bridge

If the legacy provider is "event-based" or uses a clunky messaging protocol, you can use **Spring Integration** to bridge it.

* You can define a "Gateway" interface. When you call a method on that interface, Spring Integration sends a message to a channel.
* The 3rd-party service acts as a "Service Activator" that consumes from that channel.
* **Why this helps:** It decouples your Batch threads from the legacy API's threading model (which is often the part that breaks in a Batch environment).

### 3. The "Resource" Abstraction

If the legacy API allows you to get an `InputStream` or a `File`, you can wrap it in a **Spring `Resource**`.

* Spring Batch is built to work with `Resource` objects. If you can write a small adapter that makes the legacy SFTP location look like a `Resource`, you can use all the standard Spring Batch `ItemReaders` without any custom code.

---

### The "Legacy Risk" Checklist

If you find yourself forced into the 3rd-party API, ask these three questions immediately to see if it will break our "One File / Streaming" design:

1. **Does it support Streams?** If the legacy API forces you to download to a local `java.io.File` before you can do anything, you’ll lose the "Streaming to S3" benefit. You'll need to account for local disk space (Staging Approach).
2. **Is it Thread-Safe?** Legacy clients often use static variables or non-thread-safe internal state. If so, `@JobScope` is mandatory to ensure one job doesn't step on another's toes.
3. **Does it expose "Stat" or "Rename"?** If it doesn't support renaming, our "Claim & Soft-lock" strategy will have to move to the "State Table" approach we discussed earlier.

### My Recommendation

If you have the choice, **push for the Spring-native SFTP classes.** They are built on top of `jsch` or `apache-sshd`, which are the industry standards. If you are forced into the legacy API, use the **Adapter Pattern** inside a **`@JobScope`** bean. It's the most resilient way to keep your Batch Job "pure" while dealing with "messy" underlying code.

**Would you like me to show a quick example of how to handle the `@PreDestroy` "Graceful Shutdown" for a legacy client inside a `@JobScope` bean?** It’s the best way to ensure that 3rd-party connections aren't left hanging on the SFTP server.

This is the final piece of the "Resiliency Puzzle." If you're forced to use a legacy client that doesn't manage its own connections, you have to treat that client as a "Stateful Resource."

The beauty of `@JobScope` is that Spring manages its lifecycle relative to the **Job**, not the entire application. When the job finishes, Spring "disposes" of the bean, triggering your cleanup logic.

### Legacy Adapter with Graceful Shutdown

Here is how you wrap a clunky, legacy SFTP library (let's call it `LegacySftpClient`) to ensure it never leaves a "hanging" session on the server.

```java
@Component
@JobScope
public class LegacyFileTransferAdapter implements FileTransferService {

    private final LegacySftpClient client;
    private boolean isConnected = false;

    // Use constructor to initialize with Job Parameters
    public LegacyFileTransferAdapter(
            @Value("#{jobParameters['sftp.host']}") String host,
            @Value("#{jobParameters['sftp.user']}") String user) {
        this.client = new LegacySftpClient(host, user);
    }

    private void ensureConnection() {
        if (!isConnected) {
            this.client.connect();
            this.isConnected = true;
        }
    }

    @Override
    public List<String> listFiles(String path) {
        ensureConnection();
        return client.list(path);
    }

    // ... other interface methods ...

    /**
     * This is the "Safety Valve."
     * Spring calls this automatically when the Job completes (Success or Failure).
     */
    @PreDestroy
    public void close() {
        if (isConnected) {
            try {
                System.out.println("Cleaning up legacy SFTP connection...");
                client.disconnect(); 
                isConnected = false;
            } catch (Exception e) {
                // Log and swallow - we're already shutting down
                System.err.println("Error during legacy cleanup: " + e.getMessage());
            }
        }
    }
}

```

---

### Why this is the "Pro" Move

1. **Fault Tolerance:** Even if your `transferFileStep` throws a `RuntimeException` (e.g., S3 is down), Spring Batch will still close the `@JobScope` context, which triggers `@PreDestroy`. The SSH tunnel won't leak.
2. **Lazy Connection:** By using `ensureConnection()`, you only pay the SSH handshake cost if a step actually calls a file method. If the job fails in a preliminary "Validation" step, you never even talk to the SFTP server.
3. **Encapsulation:** Your `Tasklet` code remains identical. It calls `service.listFiles()`. It has no idea it's talking to a 15-year-old JAR file instead of a modern Spring Template.

---

### Final Implementation Summary

We have covered the full spectrum:

* **The Interface:** Clean, high-level verbs.
* **The Session:** `CachingSessionFactory` (Modern) vs `LegacyAdapter` (Backup).
* **The Orchestration:** 3-step linear flow (Claim -> Transfer -> Cleanup).
* **The Audit:** A business-ready table with file sizes and status.
* **The Scope:** `@JobScope` acting as the bridge between memory and the DB.

### One Last Peer Tip for the Road

If you do use the `LegacyAdapter`, check if the 3rd-party library has its own **internal timeout**. Some legacy libraries default to "Infinite" timeout, which can hang your entire Spring Batch Job until the OS kills the thread. Always try to set a `client.setTimeout(30000)` in your constructor if the API allows it.

**You're officially ready to build. Is there any final "what if" scenario on your mind, or are you ready to close the design phase?**

































This is a great way to transition from architectural theory to a project plan. Breaking this down into a structured checklist ensures that the "nuances" we discussed don't get lost in the shuffle of coding.

---

### 1. Implementation Task List

This covers the setup and logic flow, including the SFTP environmental "gotchas."

* **SFTP Infrastructure & Connectivity**
* [ ] **SFTP Handshake:** Verify the server supports the expected SSH key or password (and that your service has network line-of-sight).
* [ ] **Permissions Check:** Ensure the SFTP user has `READ`, `WRITE` (for rename), and `DELETE` permissions in the target directory.
* [ ] **Pathing:** Confirm if paths are absolute (starting with `/`) or relative to the home directory.

* **Service Layer Development**
* [ ] **Interface Definition:** Define `FileTransferService` with the domain-specific verbs.
* [ ] **Concrete Impl:** Wrap `SftpRemoteFileTemplate` using the `CachingSessionFactory`.
* [ ] **Session Factory Config:** Configure the factory for `@JobScope` to accept `JobParameters`.

* **Spring Batch Orchestration**
* [ ] **Tasklet 1:** Implement the "Claim & Rename" logic (with the `.processing` suffix).
* [ ] **Tasklet 2:** Implement the Transfer logic using your existing S3 SDK v2 wrapper.
* [ ] **Tasklet 3:** Implement the Cleanup (SFTP Delete).
* [ ] **Job Config:** Stitch the steps together with conditional transitions.

* **Final Implementation Checklist (Audit Add-on)**

* [ ] **Schema:** Create the `file_transfer_audit` table.
* [ ] **Mapping:** Decide which `JobParameters` constitute the `client_code`.
* [ ] **Listener:** Register a `JobExecutionListener` to ensure failures are captured even if a Tasklet throws an unexpected `RuntimeException`.

---

### 2. Development & Testing Milestones

Since file transfers involve external systems, I recommend testing in "layers" to avoid chasing shadows.

* **Milestone 1: The "Ping" Test**
* Create a simple unit test that uses your `FileTransferService` to list files. This validates your `@JobScope` and `CachingSessionFactory` configuration.

* **Milestone 2: The "Claim" Test**
* Manually place a file on the SFTP server. Run only the `ClaimFileTasklet`. Verify the file is renamed to `.processing` and the name is in the `JobExecutionContext`.

* **Milestone 3: The "Pipeline" Test (Mocked S3)**
* Run the job, but have the S3 service just log the stream. This verifies the SFTP-to-Stream bridge works and the `PipedInputStream` doesn't hang.

* **Milestone 4: Negative Testing (The "Crash" Test)**
* Force an exception in the S3 transfer. Verify the job stops, the `.processing` file remains on SFTP, and the Cleanup step is **skipped**.

* **Milestone 5: End-to-End Recovery**
* Run a successful job. Verify the file is in S3 and gone from SFTP.

---

### 3. Final Implementation Review Checklist

*Consolidated tips from our discussion to keep in your back pocket.*

* **[ ] Serialization:** Ensure NO `Session`, `InputStream`, or `Channel` objects are ever put into the `ExecutionContext`. Only store Strings (filenames).
* **[ ] Bean Scoping:** Confirm the `FileTransferService` is `@JobScope` or `@StepScope` to allow for dynamic `JobParameters`.
* **[ ] Resource Cleanup:** Ensure any `OutputStream` or `PipedInputStream` is closed in a `finally` block within your Tasklet to prevent thread leaks.
* **[ ] The Rename "Soft-Lock":** Verify the `ListingTasklet` ignores files that already have the `.processing` extension (to prevent a failed job from "re-claiming" its own failure).
* **[ ] Caching Session Factory:** Check that `setTestSessionOnBorrow(true)` is set if the SFTP server is known to drop idle connections aggressively.
* **[ ] S3 Integrity:** If you choose to add the `headObject` check before SFTP deletion, ensure it's done inside the final `CleanupTasklet`.
* **[ ] Logging:** Log the `JobExecutionId` alongside the `claimed_file` name for easy cross-referencing between S3 and the Batch logs.

