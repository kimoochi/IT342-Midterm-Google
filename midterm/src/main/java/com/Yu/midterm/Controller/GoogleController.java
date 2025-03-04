package com.Yu.midterm.Controller;

import com.Yu.midterm.Model.Contact;
import com.Yu.midterm.Service.GooglePeopleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Controller
public class GoogleController {

    private final GooglePeopleService contactsService;

    public GoogleController(GooglePeopleService contactsService) {
        this.contactsService = contactsService;
    }

    @GetMapping("/contacts")
    public String getContacts(Model model, @AuthenticationPrincipal OAuth2User principal) throws GeneralSecurityException, IOException {
        String userId = principal.getName();
        model.addAttribute("contacts", contactsService.getContacts(userId));
        model.addAttribute("newContact", new Contact());
        return "contacts";
    }

    @PostMapping("/contacts/add")
    public String addContact(@ModelAttribute Contact newContact, @AuthenticationPrincipal OAuth2User principal) throws GeneralSecurityException, IOException {
        String userId = principal.getName();
        contactsService.addContact(userId, newContact);
        return "redirect:/contacts";
    }

    @GetMapping("/contacts/edit/{resourceName}")
    public String showEditForm(@PathVariable String resourceName, Model model, @AuthenticationPrincipal OAuth2User principal) throws GeneralSecurityException, IOException {
        String userId = principal.getName();
        String formattedResourceName = resourceName.replace("_", "/");
        Contact contact = contactsService.getContact(userId, formattedResourceName);
        if (contact == null || contact.getResourceName() == null) {
            System.out.println("Error: Contact not found or invalid for resourceName: " + formattedResourceName + ". Redirecting to /contacts.");
            contact = new Contact();
            contact.setResourceName(formattedResourceName);
        }
        System.out.println("Edit Form - Contact: firstName=" + contact.getFirstName() + ", lastName=" + contact.getLastName() +
                ", email=" + contact.getEmail() + ", phoneNumber=" + contact.getPhoneNumber() + ", resourceName=" + contact.getResourceName());
        model.addAttribute("contact", contact);
        model.addAttribute("resourceName", formattedResourceName);
        return "edit-contact";
    }

    @PostMapping("/contacts/update/{resourceName}")
    public String updateContact(@PathVariable String resourceName, @ModelAttribute Contact contact, @AuthenticationPrincipal OAuth2User principal) throws GeneralSecurityException, IOException {
        String userId = principal.getName();
        String formattedResourceName = resourceName.replace("_", "/");
        if (contact.getResourceName() == null) {
            contact.setResourceName(formattedResourceName);
        }
        try {
            Contact updatedContact = contactsService.updateContact(userId, formattedResourceName, contact);
            System.out.println("Update successful - Updated Contact: firstName=" + updatedContact.getFirstName() + ", lastName=" + updatedContact.getLastName() +
                    ", email=" + updatedContact.getEmail() + ", phoneNumber=" + updatedContact.getPhoneNumber());
        } catch (Exception e) {
            System.out.println("Update failed: " + e.getMessage());
            return "redirect:/contacts?error=updateFailed";
        }
        return "redirect:/contacts";
    }

    @GetMapping("/contacts/delete/{resourceName}")
    public String deleteContact(@PathVariable String resourceName, @AuthenticationPrincipal OAuth2User principal) throws GeneralSecurityException, IOException {
        String userId = principal.getName();
        contactsService.deleteContact(userId, resourceName.replace("_", "/"));
        return "redirect:/contacts";
    }
}