/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/SQLTemplate.sql to edit this template
 */
/**
 * Author:  brintoul
 * Created: Jul 17, 2025
 */
CREATE TABLE CUSTOMER (
    customer_id CHAR(36) NOT NULL,
    name VARCHAR(512),
    credit_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (customer_id)
);

CREATE TABLE USER (
    customer_id CHAR(36) NOT NULL,
    username VARCHAR(512),
    encr_code VARCHAR(512),
    PRIMARY KEY (username),
    CONSTRAINT fk_customer_user
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER (customer_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE APP_USER (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    oauth_provider VARCHAR(50) NULL,
    oauth_subject_id VARCHAR(255) NULL,
    customer_id CHAR(36) NOT NULL,
    CONSTRAINT fk_app_user_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id),
    CONSTRAINT uq_oauth_subject UNIQUE (oauth_provider, oauth_subject_id)
);

CREATE TABLE ALERT_QUEUE (
    queue_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    name VARCHAR(255),
    current_queue TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (queue_id),
    CONSTRAINT fk_customer_queue
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER (customer_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE ALERT_QUEUE_ENTRY (
    queue_id CHAR(36) NOT NULL,
    order_within_queue INT NOT NULL,
    name VARCHAR(100),
    phone_number VARCHAR(20),
    alerted TINYINT(1) NOT NULL DEFAULT 0,
    alerted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (queue_id, order_within_queue),
    CONSTRAINT fk_alert_queue_entry_queue
        FOREIGN KEY (queue_id)
        REFERENCES ALERT_QUEUE (queue_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE CUSTOMER_TRANSACTIONS (
    customer_id CHAR(36) NOT NULL,
    transaction_id VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cost DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (transaction_id),
    CONSTRAINT fk_customer_transaction
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER (customer_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE ALERT_MESSAGE (
    message_id INT NOT NULL AUTO_INCREMENT,
    queue_id CHAR(36) NOT NULL,
    message VARCHAR(1024) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (message_id)
);

CREATE TABLE APPOINTMENT (
    appointment_id CHAR(36) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    appointment_time DATETIME NOT NULL,
    customer_id CHAR(36) NOT NULL,
    PRIMARY KEY (appointment_id),
    CONSTRAINT fk_appointment_customer
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER (customer_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE PERSON_OR_ENTITY (
    id CHAR(36) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    customer_id CHAR(36) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_person_name_phone UNIQUE (first_name, last_name, phone_number),
    CONSTRAINT fk_pe_customer
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER (customer_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE CONTACT_LIST (
    id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_id CHAR(36) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contact_list_customer
        FOREIGN KEY (customer_id)
        REFERENCES CUSTOMER (customer_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE CONTACT_LIST_PERSON (
    contact_list_id CHAR(36) NOT NULL,
    person_or_entity_id CHAR(36) NOT NULL,
    PRIMARY KEY (contact_list_id, person_or_entity_id),
    CONSTRAINT fk_clp_contact_list
        FOREIGN KEY (contact_list_id)
        REFERENCES CONTACT_LIST (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_clp_person
        FOREIGN KEY (person_or_entity_id)
        REFERENCES PERSON_OR_ENTITY (id)
        ON DELETE CASCADE
);