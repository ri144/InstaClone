package com.example.demo.validators;


import com.example.demo.models.User;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import org.springframework.validation.Validator;

@Component
public class UserValidator implements Validator {

    @Autowired
    UserRepository userRepository;

    public boolean supports(Class<?> clazz){
        return User.class.isAssignableFrom(clazz);
    }

    public void validate(Object target, Errors errors){
        User user = (User) target;

        String email = user.getEmail();
        String username = user.getUsername();
        String password = user.getPassword();

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "firstName", "user.firstName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "lastName", "user.lastName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "username", "user.username.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "email", "user.email.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "user.password.empty");

        if(username.length() < 5){
            errors.rejectValue("username","user.username.tooShort");
        }
        if(password.length() < 5){
            errors.rejectValue("password","user.password.tooShort");
        }
        boolean found = false;
        for(char c : password.toCharArray()){
            if(Character.isDigit(c)){
                found = true;
                break;
            }
        }
        if(!found){
            errors.rejectValue("password","user.password.noNums");
        }
        if(password.equals(password.toLowerCase())){
            errors.rejectValue("password","user.password.noCaps");
        }
        if(password.equals(password.toUpperCase())){
            errors.rejectValue("password","user.password.noLows");
        }
        if(password.equals(username)){
            errors.rejectValue("password","user.password.sameUser");
            errors.rejectValue("username","user.password.sameUser");
        }
        if(userRepository.countByEmail(email)>0){
            errors.rejectValue("email", "user.email.duplicate");
        }
        if(userRepository.countByUsername(username)>0){
            errors.rejectValue("username", "user.username.duplicate");
        }
    }
}
