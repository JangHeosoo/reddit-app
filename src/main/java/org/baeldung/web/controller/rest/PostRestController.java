package org.baeldung.web.controller.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.baeldung.persistence.dao.PostRepository;
import org.baeldung.persistence.model.Post;
import org.baeldung.persistence.model.User;
import org.baeldung.reddit.classifier.RedditClassifier;
import org.baeldung.web.exceptions.InvalidDateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(value = "/api")
public class PostRestController {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final SimpleDateFormat dfHour = new SimpleDateFormat("HH");
    private static final int PAGE_SIZE = 2;

    @Autowired
    private PostRepository postReopsitory;

    @Autowired
    private RedditClassifier redditClassifier;

    // === API Methods

    @RequestMapping(value = "/scheduledPosts", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void schedule(@RequestBody final Post post, @RequestParam(value = "date") final String date) throws ParseException {
        post.setSubmissionDate(calculateSubmissionDate(date, getCurrentUser().getPreference().getTimezone()));
        if (post.getSubmissionDate().before(new Date())) {
            throw new InvalidDateException("Scheduling Date already passed");
        }
        post.setUser(getCurrentUser());
        post.setSubmissionResponse("Not sent yet");
        postReopsitory.save(post);
    }

    @RequestMapping(value = "/scheduledPosts/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void deletePost(@PathVariable("id") final Long id) {
        postReopsitory.delete(id);
    }

    @RequestMapping(value = "/scheduledPosts/{id}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void updatePost(@RequestBody final Post post, @RequestParam(value = "date") final String date) throws ParseException {
        post.setSubmissionDate(calculateSubmissionDate(date, getCurrentUser().getPreference().getTimezone()));
        if (post.getSubmissionDate().before(new Date())) {
            throw new InvalidDateException("Scheduling Date already passed");
        }
        post.setUser(getCurrentUser());
        postReopsitory.save(post);
    }

    @RequestMapping(value = "/predicatePostResponse", method = RequestMethod.POST)
    @ResponseBody
    public final String predicatePostResponse(@RequestParam(value = "title") final String title, @RequestParam(value = "domain") final String domain) {
        final int hour = Integer.parseInt(dfHour.format(new Date()));
        final int result = redditClassifier.classify(redditClassifier.convertPost(title, domain, hour));
        return (result == RedditClassifier.GOOD) ? "{Good Response}" : "{Bad response}";
    }

    @RequestMapping("/scheduledPosts")
    @ResponseBody
    public final List<Post> getScheduledPosts(@RequestParam(value = "page", required = false) final int page) {
        final User user = getCurrentUser();
        final Page<Post> posts = postReopsitory.findByUser(user, new PageRequest(page, PAGE_SIZE));
        return posts.getContent();
    }

    // === private

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Date calculateSubmissionDate(final String dateString, final String userTimeZone) throws ParseException {
        dateFormat.setTimeZone(TimeZone.getTimeZone(userTimeZone));
        return dateFormat.parse(dateString);
    }

}
