package com.controlledthinking.dto;

public class ContactListUploadResponse {

    private final String id;
    private final String name;
    private final int imported;
    private final int skipped;

    public ContactListUploadResponse(String id, String name, int imported, int skipped) {
        this.id = id;
        this.name = name;
        this.imported = imported;
        this.skipped = skipped;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getImported() { return imported; }
    public int getSkipped() { return skipped; }
}
