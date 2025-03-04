package com.Yu.midterm.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
import com.Yu.midterm.Model.Contact;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GooglePeopleService {

    private final OAuth2AuthorizedClientService clientService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    public GooglePeopleService(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    private PeopleService getPeopleService(String userId) throws GeneralSecurityException, IOException {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", userId);
        if (client == null) {
            throw new GeneralSecurityException("No authorized client found for userId: " + userId);
        }
        OAuth2AccessToken accessToken = client.getAccessToken();

        Credential credential = new GoogleCredential().setAccessToken(accessToken.getTokenValue());
        return new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Google Contacts App")
                .build();
    }

    public List<Contact> getContacts(String userId) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(userId);
        ListConnectionsResponse response = peopleService.people().connections()
                .list("people/me")
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .execute();
        List<Person> persons = response.getConnections();
        if (persons == null) {
            return Collections.emptyList();
        }

        List<Contact> contacts = new ArrayList<>();
        for (Person person : persons) {
            contacts.add(mapPersonToContact(person));
        }
        return contacts;
    }

    public Contact addContact(String userId, Contact contact) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(userId);
        Person person = mapContactToPerson(contact);
        Person createdPerson = peopleService.people().createContact(person).execute();
        return mapPersonToContact(createdPerson);
    }

    public Contact updateContact(String userId, String resourceName, Contact contact) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(userId);
        Person person = mapContactToPerson(contact);

        // Fetch the existing person without etag in personFields (etag is implicit)
        Person existingPerson = peopleService.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers") // Removed etag from personFields
                .execute();
        if (existingPerson == null) {
            System.out.println("Error: Failed to retrieve existing person for resourceName: " + resourceName);
            throw new IOException("Failed to retrieve existing person for update");
        }
        // Use the etag from the response (implicitly included)
        if (existingPerson.getEtag() == null) {
            System.out.println("Error: Etag is null for resourceName: " + resourceName);
            throw new IOException("Etag is null for update");
        }
        person.setEtag(existingPerson.getEtag()); // Set the etag for optimistic locking

        // Log the person object before update with detailed field values
        String givenName = (person.getNames() != null && !person.getNames().isEmpty() && person.getNames().get(0) != null) ? person.getNames().get(0).getGivenName() : "null";
        String familyName = (person.getNames() != null && !person.getNames().isEmpty() && person.getNames().get(0) != null) ? person.getNames().get(0).getFamilyName() : "null";
        String email = (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty() && person.getEmailAddresses().get(0) != null) ? person.getEmailAddresses().get(0).getValue() : "null";
        String phone = (person.getPhoneNumbers() != null && !person.getPhoneNumbers().isEmpty() && person.getPhoneNumbers().get(0) != null) ? person.getPhoneNumbers().get(0).getValue() : "null";
        System.out.println("Updating Contact - Person: givenName=" + givenName + ", familyName=" + familyName +
                ", email=" + email + ", phone=" + phone + ", etag=" + person.getEtag());

        // Perform the update
        try {
            Person updatedPerson = peopleService.people().updateContact(resourceName, person)
                    .setUpdatePersonFields("names,emailAddresses,phoneNumbers")
                    .execute();

            // Log the result with detailed field values
            String updatedGivenName = (updatedPerson.getNames() != null && !updatedPerson.getNames().isEmpty() && updatedPerson.getNames().get(0) != null) ? updatedPerson.getNames().get(0).getGivenName() : "null";
            String updatedFamilyName = (updatedPerson.getNames() != null && !updatedPerson.getNames().isEmpty() && updatedPerson.getNames().get(0) != null) ? updatedPerson.getNames().get(0).getFamilyName() : "null";
            String updatedEmail = (updatedPerson.getEmailAddresses() != null && !updatedPerson.getEmailAddresses().isEmpty() && updatedPerson.getEmailAddresses().get(0) != null) ? updatedPerson.getEmailAddresses().get(0).getValue() : "null";
            String updatedPhone = (updatedPerson.getPhoneNumbers() != null && !updatedPerson.getPhoneNumbers().isEmpty() && updatedPerson.getPhoneNumbers().get(0) != null) ? updatedPerson.getPhoneNumbers().get(0).getValue() : "null";
            System.out.println("Update successful - Updated Person: givenName=" + updatedGivenName + ", familyName=" + updatedFamilyName +
                    ", email=" + updatedEmail + ", phone=" + updatedPhone + ", etag=" + updatedPerson.getEtag());

            return mapPersonToContact(updatedPerson);
        } catch (IOException e) {
            System.out.println("Update failed for resourceName: " + resourceName + " - " + e.getMessage());
            throw e; // Re-throw to be caught by the controller
        }
    }

    public void deleteContact(String userId, String resourceName) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(userId);
        peopleService.people().deleteContact(resourceName).execute();
    }

    public Contact getContact(String userId, String resourceName) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(userId);
        try {
            Person person = peopleService.people().get(resourceName)
                    .setPersonFields("names,emailAddresses,phoneNumbers") // Removed etag from personFields
                    .execute();
            if (person == null) {
                System.out.println("Error: Google API returned null Person for resourceName: " + resourceName);
                return new Contact();
            }
            Contact contact = mapPersonToContact(person);
            if (contact.getResourceName() == null) {
                contact.setResourceName(resourceName);
            }
            System.out.println("getContact - Mapped Contact: firstName=" + contact.getFirstName() + ", lastName=" + contact.getLastName() +
                    ", email=" + contact.getEmail() + ", phoneNumber=" + contact.getPhoneNumber() + ", resourceName=" + contact.getResourceName());
            return contact;
        } catch (IOException e) {
            System.out.println("Error fetching contact with resourceName: " + resourceName + " - " + e.getMessage());
            Contact fallbackContact = new Contact();
            fallbackContact.setResourceName(resourceName);
            return fallbackContact;
        }
    }

    private Contact mapPersonToContact(Person person) {
        Contact contact = new Contact();
        contact.setResourceName(person.getResourceName() != null ? person.getResourceName() : "");

        if (person.getNames() != null && !person.getNames().isEmpty() && person.getNames().get(0) != null) {
            contact.setFirstName(person.getNames().get(0).getGivenName() != null ? person.getNames().get(0).getGivenName() : "");
            contact.setLastName(person.getNames().get(0).getFamilyName() != null ? person.getNames().get(0).getFamilyName() : "");
        } else {
            contact.setFirstName("");
            contact.setLastName("");
        }
        if (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty() && person.getEmailAddresses().get(0) != null) {
            contact.setEmail(person.getEmailAddresses().get(0).getValue() != null ? person.getEmailAddresses().get(0).getValue() : "");
        } else {
            contact.setEmail("");
        }
        if (person.getPhoneNumbers() != null && !person.getPhoneNumbers().isEmpty() && person.getPhoneNumbers().get(0) != null) {
            contact.setPhoneNumber(person.getPhoneNumbers().get(0).getValue() != null ? person.getPhoneNumbers().get(0).getValue() : "");
        } else {
            contact.setPhoneNumber("");
        }
        return contact;
    }

    private Person mapContactToPerson(Contact contact) {
        Person person = new Person();
        if (contact.getFirstName() != null || contact.getLastName() != null) {
            Name name = new Name()
                    .setGivenName(contact.getFirstName() != null && !contact.getFirstName().isEmpty() ? contact.getFirstName() : null)
                    .setFamilyName(contact.getLastName() != null && !contact.getLastName().isEmpty() ? contact.getLastName() : null);
            person.setNames(Collections.singletonList(name));
        } else {
            // Ensure at least one name is set to avoid API rejection
            Name name = new Name().setGivenName(contact.getFirstName() != null && !contact.getFirstName().isEmpty() ? contact.getFirstName() : "Unknown");
            person.setNames(Collections.singletonList(name));
        }
        if (contact.getEmail() != null && !contact.getEmail().isEmpty()) {
            EmailAddress email = new EmailAddress().setValue(contact.getEmail());
            person.setEmailAddresses(Collections.singletonList(email));
        }
        if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()) {
            PhoneNumber phone = new PhoneNumber().setValue(contact.getPhoneNumber());
            person.setPhoneNumbers(Collections.singletonList(phone));
        }
        return person;
    }
}