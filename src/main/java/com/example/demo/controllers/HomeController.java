package com.example.demo.controllers;


import com.cloudinary.Singleton;
import com.cloudinary.StoredFile;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.configs.CloudinaryConfig;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
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

import static com.example.demo.configs.MergeSort.sortPhotos;

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

    @Autowired
    private LikesRepo likeRepo;

    @RequestMapping("/")
    public String index(Principal principal, Model model){
        User u = userService.findByUsername(principal.getName());
        model = setupProfile(u.getId(), model, u);
        model.addAttribute("followcheck", false);
        model.addAttribute("unfollowcheck", false);
        model.addAttribute("user", u.getUsername());
        return "profile";
    }

    @RequestMapping("/profile/{id}")
    public String goToProfile(@PathVariable("id") Long id, Model model, Principal principal){
        User u = userService.findByUsername(principal.getName());
        Long id2 = u.getId();
        model = setupProfile(id, model, u);
        if(id2 != id) {
            if(followerRepo.findByUseridAndPersonid(id2, id) == null) {
                model.addAttribute("followcheck", true);
                model.addAttribute("unfollowcheck", false);
            }
            else{
                model.addAttribute("followcheck", false);
                model.addAttribute("unfollowcheck", true);
            }
            model.addAttribute("id", id);
        }
        else{
            model.addAttribute("followcheck", false);
            model.addAttribute("unfollowcheck", false);
        }
        model.addAttribute("user", userService.findbyUserid(id).getUsername());
        return "profile";
    }

    @RequestMapping("follow/{id}")
    public String follow(@PathVariable("id") Long id, Model model, Principal principal){
        User u = userService.findByUsername(principal.getName());
        Long id2 = u.getId();
        List<Follower> flist = followerRepo.findAllByUserid(id2);
        boolean check = true;
        for(Follower f : flist){
            if(f.getPersonid() == id){
                check = false;
                break;
            }
        }
        if(check){
            Follower f = new Follower();
            f.setUserid(id2);
            f.setPersonid(id);
            followerRepo.save(f);
        }
        model = setupProfile(id, model,u);
        model.addAttribute("followcheck", false);
        model.addAttribute("unfollowcheck", true);
        model.addAttribute("user", userService.findbyUserid(id).getUsername());
        return "profile";
    }

    @RequestMapping("unfollow/{id}")
    public String unfollow(@PathVariable("id") Long id, Model model, Principal principal){
        User u = userService.findByUsername(principal.getName());
        Long id2 = u.getId();
        followerRepo.deleteByUseridAndPersonid(id2, id);
        model = setupProfile(id, model, u);
        model.addAttribute("followcheck", true);
        model.addAttribute("unfollowcheck", false);
        model.addAttribute("user", userService.findbyUserid(id).getUsername());
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

    @PostMapping("/Search")
    public String getUser(@RequestParam("search") String search, Model model){
        List<User> ulist = userService.findUsersLike(search);
        if(ulist.isEmpty()){
            model.addAttribute("emptymsg", true);
        }
        else{
            model.addAttribute("emptymsg", false);
        }
        model.addAttribute("list", ulist);
        return "results";
    }

    public UserValidator getUserValidator() {
        return userValidator;
    }

    public void setUserValidator(UserValidator userValidator) {
        this.userValidator = userValidator;
    }

    @RequestMapping("/feed/{value}")
    public String gotoFeed(Model model, Principal principal, @PathVariable("value") int value){
        if(value == 0){
            value = 1;
        }
        Long id = userService.findByUsername(principal.getName()).getId();
        List<Follower> flist = followerRepo.findAllByUserid(id);
        List<Photo> pList = new ArrayList<Photo>();
        for(Follower f : flist){
            pList.addAll(photoRepo.findAllByUserid(f.getPersonid()));
        }
        pList.sort(Comparator.comparing(Photo::getCreatedAt).reversed()); //sort by date
        double values = Math.ceil(pList.size()/10);
        List<Photo> pList2 = new ArrayList<Photo>();
        for(int i = (value-1)*10;i < (value-1)*10+10; i++){
            try {
                pList2.add(pList.get(i));
            }
            catch(IndexOutOfBoundsException e){
                break;
            }
        }
        List<Integer> list = new ArrayList<Integer>();
        for(int i = 1;i<=values+1;i++){
            list.add(i);
        }
        if(pList2.isEmpty()){
            model.addAttribute("emptymsg", true);
        }
        else{
            model.addAttribute("emptymsg", false);
        }
        model.addAttribute("values", list);
        model.addAttribute("valuePrev", value-1);
        model.addAttribute("valueNext", value+1);
        model.addAttribute("myimages", pList2);
        return "feed";
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
            model.addAttribute("edits", "bo_2px_solid_black,c_fill,h_200,w_200");
            // photoRepo.save(p);
        } catch (IOException e){
            e.printStackTrace();
            model.addAttribute("message", "Sorry I can't upload that!");
        }
        return "confirmphoto";
    }

    @PostMapping("submitPic/{edits}/{pic}")
    public String submitPic(Principal principal, Model model, @PathVariable("edits") String edits, @PathVariable("pic") String filename){
        String username = principal.getName();
        User u = userService.findByUsername(username);
        Long id = u.getId();
        Photo photo = new Photo();
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/" + edits + "/" + filename + ".jpg'/>");
        photo.setUserid(id);
        photo.setCreatedAt(new Date());
        photo.setUsername(username);
        photoRepo.save(photo);
        model = setupProfile(id, model, u);
        model.addAttribute("followcheck", false);
        model.addAttribute("unfollowcheck", false);
        model.addAttribute("user", u.getUsername());
        return "profile";
    }

    @RequestMapping("/filter/red/{filename}")
    public String filterRed(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/e_colorize:50,co_rgb:ff0000,bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "e_colorize:50,co_rgb:ff0000,bo_2px_solid_black,c_fill,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/filter/none/{filename}")
    public String filterNone(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "bo_2px_solid_black,c_fill,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/filter/hue/{filename}")
    public String filterHue(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/e_hue,bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "e_hue,bo_2px_solid_black,c_fill,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/filter/sepia/{filename}")
    public String filterSepia(@ModelAttribute("images") Photo photo, Model model, @PathVariable("filename") String filename){
        photo.setImage("<img src='http://res.cloudinary.com/dmzhobs8c/image/upload/e_sepia,bo_2px_solid_black,c_fit,h_250,w_250/"+filename+".jpg'/>");
        model.addAttribute("filename", filename);
        model.addAttribute("images", photo);
        model.addAttribute("pic", filename);
        model.addAttribute("edits", "e_sepia,bo_2px_solid_black,c_fill,h_200,w_200");
        return "confirmphoto";
    }

    @RequestMapping("/img/{id}")
    public String imgDisplay(Model model, @PathVariable("id") Long id, Principal principal){
        User user = userService.findByUsername(principal.getName());
        model = setupPicturePage(model, id, user);
        Likes like = likeRepo.findByPhotoidAndUserid(id, user.getId());
        if(like == null) {
            model.addAttribute("like", true);
        }
        else{
            model.addAttribute("like", false);
        }
        return "gallery";
    }

    @PostMapping("/newComment/{id}")
    public String addComment(@PathVariable("id") Long id, Model model, @ModelAttribute Comment comment, Principal principal){
        comment.setPhotoid(id);
        User user = userService.findByUsername(principal.getName());
        comment.setUsername(user.getUsername());
        comment.setUserid(user.getId());
        commentRepo.save(comment);
        model = setupPicturePage(model, id, user);
        if(likeRepo.findByPhotoidAndUserid(id, userService.findByUsername(principal.getName()).getId()) == null)
            model.addAttribute("like", true);
        else{
            model.addAttribute("like", false);
        }
        return "gallery";
    }

    @RequestMapping("incrementCounter/{id}")
    public String incrementCounter(@PathVariable("id") Long id, Model model, Principal principal){
        User user = userService.findByUsername(principal.getName());
        likeRepo.save(new Likes(id, user.getId()));
        Photo p = photoRepo.findById(id);
        List<Likes> list = likeRepo.findAllByPhotoid(id);
        p.setLikecounter(list.size());
        photoRepo.save(p);
        model = setupPicturePage(model, id, user);
        model.addAttribute("like", false);
        return "gallery";
    }

    @RequestMapping("decrementCounter/{id}")
    public String decrementCounter(@PathVariable("id") Long id, Model model, Principal principal){
        User user = userService.findByUsername(principal.getName());
        likeRepo.deleteByUserid(user.getId());
        Photo p = photoRepo.findById(id);
        List<Likes> list = likeRepo.findAllByPhotoid(id);
        p.setLikecounter(list.size());
        photoRepo.save(p);
        model = setupPicturePage(model, id, user);
        model.addAttribute("like", true);
        return "gallery";
    }

    @RequestMapping("editComment/{id}")
    public String editComm(@PathVariable("id") Long id, Model model, Principal principal){
        User user = userService.findByUsername(principal.getName());
        Comment c = commentRepo.findById(id);
        if(likeRepo.findByPhotoidAndUserid(c.getPhotoid(), user.getId()) == null)
            model.addAttribute("like", true);
        else{
            model.addAttribute("like", false);
        }
        model = setupPicturePage(model, id, user);
        return "gallery";
    }

    private Model setupPicturePage(Model model, Long id, User user){
        Photo p = photoRepo.findById(id);
        p.setImage(p.getImage().replaceAll("fill","fit").replaceAll("200","400"));
        model.addAttribute("images", p);
        List<Comment> clist = commentRepo.findAllByPhotoid(id);
        model.addAttribute("myid", user.getId());
        model.addAttribute("comments", clist);
        model.addAttribute("newcomment", new Comment());
        return model;
    }

    private Model setupProfile(Long id, Model model, User user){
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
