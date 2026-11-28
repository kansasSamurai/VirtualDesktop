package org.katacode.agents.auditor.ui;

/**
 * Detail view for one vendor. So far only the header, with the mocked Cursor
 * vendor.
 */
@SuppressWarnings("serial")
class VendorDetailPanel extends ViewPanel {

    VendorDetailPanel() {
        showHeader("VENDOR HEALTH: Cursor", ".cursor/rules/");
    }
}
