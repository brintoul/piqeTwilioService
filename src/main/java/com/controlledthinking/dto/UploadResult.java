package com.controlledthinking.dto;

import com.controlledthinking.db.PersonOrEntity;
import java.util.List;

public class UploadResult {

    private final List<PersonOrEntity> people;
    private final int imported;
    private final int skipped;

    public UploadResult(List<PersonOrEntity> people, int imported, int skipped) {
        this.people = people;
        this.imported = imported;
        this.skipped = skipped;
    }

    public List<PersonOrEntity> getPeople() { return people; }
    public int getImported() { return imported; }
    public int getSkipped() { return skipped; }
}
