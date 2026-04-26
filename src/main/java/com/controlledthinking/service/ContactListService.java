package com.controlledthinking.service;

import com.controlledthinking.dao.ContactListDAO;
import com.controlledthinking.db.ContactList;
import com.controlledthinking.dto.ContactListUploadResponse;
import com.controlledthinking.dto.UploadResult;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

public class ContactListService {

    private final ContactListDAO clDao;
    private final PersonOrEntityService peService;

    public ContactListService(ContactListDAO clDao, PersonOrEntityService peService) {
        this.clDao = clDao;
        this.peService = peService;
    }

    public ContactListUploadResponse createFromUpload(String name, InputStream inputStream, UUID customerId) throws IOException {
        UploadResult result = peService.parseAndUpsert(inputStream, customerId);

        ContactList list = new ContactList();
        list.setName(name);
        list.setUploadedAt(LocalDateTime.now());
        list.setCustomer(clDao.getCustomerReference(customerId));
        list.setPeople(result.getPeople());

        clDao.create(list);

        return new ContactListUploadResponse(list.getId(), list.getName(), result.getImported(), result.getSkipped());
    }
}
