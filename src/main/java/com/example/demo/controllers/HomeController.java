package com.example.demo.controllers;


import com.cloudinary.Singleton;
import com.cloudinary.StoredFile;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.configs.CloudinaryConfig;
import com.example.demo.models.Comment;
import com.example.demo.models.Follower;
import com.example.demo.models.Photo;
import com.example.demo.models.User;
import com.example.demo.repositories.CommentRepo;
import com.example.demo.repositories.FollowerRepo;
import com.example.demo.repositories.PhotoRepository;
import com.example.demo.services.UserService;
import com.example.demo.validators.UserValidator;
import com.google.common.collect.Lists;
import it.ozimov.springboot.mail.model.Email;
import it.ozimov.springboot.mail.model.defaultimpl.DefaultEmail;
import it.ozimov.springboot.mail.service.EmailService;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.cloudinary.Cloudinary;

import javax.mail.internet.InternetAddress;
import javax.validation.Valid;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    CloudinaryConfig cloudc;

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private UserService userService;

    @Autowired
    private PhotoRepository photoRepo;

    @Autowired
    private FollowerRepo followerRepo;

    @Autowired
    private CommentRepo commentRepo;

    @RequestMapping("/")
    public String index(Principal principal, Model model){
        User u = userService.findByUsername(principal.getName());
        model = setupProfile(u.getId(), model);
        model.addAttribute("followcheck", false);
        return "profile";
    }

    @RequestMapping("/profile/{id}")
    public String goToProfile(@PathVariable("id") Long id, Model model){
        model = setupProfile(id, model);
        model.addAttribute("followcheck", true);
        return "profile";
    }

    @RequestMapping("/login")
    public String login(){
        return "login";
    }

    @RequestMapping(value="/register", method = RequestMethod.GET)
    public String showRegistrationPage(Model model){
        model.addAttribute("user", new User());
        return "registration";
    }

    @RequestMapping(value="/register", method = RequestMethod.POST)
    public String processRegistrationPage(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) throws UnsupportedEncodingException {

        model.addAttribute("user", user);
        userValidator.validate(user, result);

        if (result.hasErrors()) {
            return "registration";
        } else {
            userService.saveUser(user);
            model.addAttribute("message", "User Account Successfully Created");
        }

        return "login";
    }

    public UserValidator getUserValidator() {
        return userValidator;
    }

    public void setUserValidator(UserValidator userValidator) {
        this.userValidator = userValidator;
    }


    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("p", new Photo());
        return "upload";
    }

    @PostMapping("/upload")
    public String singleImageUpload(@RequestParam("file") MultipartFile file, Principal principal,
                                    RedirectAttributes redirectAttributes, Model model, @ModelAttribute Photo p){
        Long id = userService.findByUsername(principal.getName()).getId();
        if (file.isEmpty()){
            redirectAttributes.addFlashAttribute("message","Please select a file to upload");
            return "redirect:uploadStatus";
        }

        try {
            Map uploadResult =  cloudc.upload(file.getBytes(), ObjectUtils.asMap("resourcetype", "auto"));

            model.addAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");
            String filename = uploadResult.get("public_id").toString() + "." + uploadResult.get("format").toString();
            p.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/"+filename+"' width='300px'/>");
            //System.out.printf("%s\n", cloudc.createUrl(filename,900,900, "fit"));
            p.setCreatedAt(new Date());
            p.setUserid(id);
            photoRepo.save(p);
        } catch (IOException e){
            e.printStackTrace();
            model.addAttribute("message", "Sorry I can't upload that!");
        }
        model = setupProfile(id, model);
        model.addAttribute("followcheck", false);
        return "profile";
    }

    @RequestMapping("/img/{id}")
    public String imgDisplay(Model model, @PathVariable("id") Long id){
        Photo p = photoRepo.findById(id);
        model.addAttribute("images", p);
        List<Comment> clist = commentRepo.findAllByPhotoid(id);
        model.addAttribute("comment", clist);
        model.addAttribute("newcomment", new Comment());
        return "gallery";
    }

    @PostMapping("/newComment/{id}")
    public String addComment(@PathVariable("id") Long id, Model model){
        Photo p = photoRepo.findById(id);
        model.addAttribute("images", p);
        List<Comment> clist = commentRepo.findAllByPhotoid(id);
        model.addAttribute("comment", clist);
        model.addAttribute("newcomment", new Comment());
        return "gallery";
    }

    private Model setupProfile(Long id, Model model){
        List<Photo> pList = photoRepo.findAllByUserid(id);
        model.addAttribute("myphotoList", pList);
        List<Follower> flist = followerRepo.findAllByUserid(id);
        List<User> uList = new ArrayList<User>();
        for(Follower f : flist) {
            uList.add(userService.findbyUserid(f.getPersonid()));
        }
        model.addAttribute("followList", uList);
        return model;
    }

    @Autowired
    public EmailService emailService;
    public void sendEmailWithoutTemplating(String username, String email2, Long id) throws UnsupportedEncodingException {
        final Email email = DefaultEmail.builder()
                .from(new InternetAddress("daylinzack@gmail.com", "Admin Darth Vader"))
                .to(Lists.newArrayList(new InternetAddress(email2, username)))
                .subject("Your meme is here and ready for consumption")
                .body("Hi youre meme is: localhost:9000/memelink/" + String.valueOf(id) )
                .encoding("UTF-8").build();
        emailService.send(email);
    }

}
