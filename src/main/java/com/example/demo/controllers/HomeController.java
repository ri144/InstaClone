package com.example.demo.controllers;


import com.cloudinary.Singleton;
import com.cloudinary.StoredFile;
import com.cloudinary.Transformation;
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

            p.setImage(cloudc.createUrl(filename, 250, 250, "fit"));
            //System.out.printf("%s\n", "<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/"+filename+"' width='250px'/>");
            // <img src='http://res.cloudinary.com/dmzhobs8c/image/upload/bo_2px_solid_black,c_fit,h_250,w_250/kolgnlmn0ywupoeg0glm.jpg'/>
            model.addAttribute("images", p);
            model.addAttribute("filename", filename);
            model.addAttribute("pic", filename);
            model.addAttribute("edits", "bo_2px_solid_black,c_fit,h_200,w_200");
            // photoRepo.save(p);
        } catch (IOException e){
            e.printStackTrace();
            model.addAttribute("message", "Sorry I can't upload that!");
        }
        return "confirmphoto";
    }

    @PostMapping("submitPic/{edits}/{pic}")
    public String submitPic(Principal principal, Model model, @PathVariable("edits") String edits, @PathVariable("pic") String filename){
        Long id = userService.findByUsername(principal.getName()).getId();
        Photo photo = new Photo();
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/" + edits + "/" + filename + ".jpg'/>");
        photo.setUserid(id);
        photo.setCreatedAt(new Date());
        photoRepo.save(photo);
        model = setupProfile(id, model);
        model.addAttribute("followcheck", false);
        return "profile";
    }

    @RequestMapping("/filter/red/{filename}")
    public String filterRed(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/e_colorize:50,co_rgb:ff0000,bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "e_colorize:50,co_rgb:ff0000,bo_2px_solid_black,c_fit,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/filter/none/{filename}")
    public String filterNone(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "bo_2px_solid_black,c_fit,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/filter/hue/{filename}")
    public String filterHue(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/e_hue,bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "e_hue,bo_2px_solid_black,c_fit,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/filter/sepia/{filename}")
    public String filterSepia(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/e_sepia,bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "e_sepia,bo_2px_solid_black,c_fit,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/img/{id}")
    public String imgDisplay(Model model, @PathVariable("id") Long id){
        model = setupPicturePage(model, id);
        return "gallery";
    }

    @PostMapping("/newComment/{id}")
    public String addComment(@PathVariable("id") Long id, Model model, @ModelAttribute Comment comment, Principal principal){
        comment.setPhotoid(id);
        comment.setUsername(userService.findByUsername(principal.getName()).getUsername());
        commentRepo.save(comment);
        model = setupPicturePage(model, id);
        return "gallery";
    }

    @RequestMapping("incrementCounter/{id}")
    public String incrementCounter(@PathVariable("id") Long id, Model model){
        Photo p = photoRepo.findById(id);
        p.setLikecounter(p.getLikecounter() + 1);
        photoRepo.save(p);
        model = setupPicturePage(model, id);
        return "gallery";
    }

    private Model setupPicturePage(Model model, Long id){
        Photo p = photoRepo.findById(id);
        model.addAttribute("images", p);
        List<Comment> clist = commentRepo.findAllByPhotoid(id);
        model.addAttribute("comments", clist);
        model.addAttribute("newcomment", new Comment());
        return model;
    }

    private Model setupProfile(Long id, Model model){
        List<Photo> pList = photoRepo.findAllByUserid(id);
        model.addAttribute("myimages", pList);
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
