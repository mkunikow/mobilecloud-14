package org.magnum.mobilecloud.video.controler;

import org.magnum.mobilecloud.video.client.VideoSvcApi;
import org.magnum.mobilecloud.video.repository.Video;
import org.magnum.mobilecloud.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collection;
import java.util.Set;

/**
 * Created by michal on 8/24/14.
 */

@Controller
public class VideoController {

    private static final String ID_PARAMETER = "id";

    @ResponseStatus(value = org.springframework.http.HttpStatus.NOT_FOUND)
    private final class ResourceNotFoundException extends RuntimeException {
        //  class definition
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    private final class BadReqeustException extends RuntimeException {
        //  class definition
    }

    // The VideoRepository that we are going to store our videos
    // in. We don't explicitly construct a VideoRepository, but
    // instead mark this object as a dependency that needs to be
    // injected by Spring. Our Application class has a method
    // annotated with @Bean that determines what object will end
    // up being injected into this member variable.
    //
    // Also notice that we don't even need a setter for Spring to
    // do the injection.
    //
    @Autowired
    private VideoRepository videos;

    @RequestMapping(value= VideoSvcApi.VIDEO_SVC_PATH + "/{" + ID_PARAMETER + "}/like", method= RequestMethod.POST)
    public @ResponseBody Video likeVideo(@PathVariable(ID_PARAMETER) long id, Principal p) {
        Video video = videos.findOne(id);
        if (video == null) {
            throw new ResourceNotFoundException();
        }

        String username = p.getName();
        Set usernames = video.getLikeUserNames();

        if (usernames.contains(username)) {
            throw new BadReqeustException();
        }

        usernames.add(username);
        video.setLikes(video.getLikes() + 1);
        videos.save(video);
        return video;
    }

    @RequestMapping(value= VideoSvcApi.VIDEO_SVC_PATH + "/{" + ID_PARAMETER + "}/unlike", method= RequestMethod.POST)
    public @ResponseBody Video unlikeVideo(@PathVariable(ID_PARAMETER) long id, Principal p) {

        Video video = videos.findOne(id);
        if (video == null) {
            throw new ResourceNotFoundException();
        }

        String username = p.getName();
        Set usernames = video.getLikeUserNames();

        if (!usernames.contains(username)) {
            throw new BadReqeustException();
        }

        usernames.remove(username);
        video.setLikes(video.getLikes() - 1);
        videos.save(video);
        return video;

    }

    @RequestMapping(value= VideoSvcApi.VIDEO_SVC_PATH + "/{" + ID_PARAMETER + "}/likedby", method= RequestMethod.GET)
    public @ResponseBody Set<Video> getUsersWhoLikedVideo(@PathVariable(ID_PARAMETER) long id) {

        Video video = videos.findOne(id);
        if (video == null) {
            throw new ResourceNotFoundException();
        }

        return video.getLikeUserNames();
    }

    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video v) {
        videos.save(v);
        return v;
    }

    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method= RequestMethod.GET)
    public @ResponseBody Iterable<Video> getVideoList() {
        return videos.findAll();
    }

    @RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH + "/{" + ID_PARAMETER + "}",  method= RequestMethod.GET)
    public @ResponseBody Video getVideo(@PathVariable(ID_PARAMETER) long id) {
        return videos.findOne(id);
    }

    @RequestMapping(value=VideoSvcApi.VIDEO_TITLE_SEARCH_PATH, method = RequestMethod.GET)
    public @ResponseBody Collection<Video> findByName(@RequestParam(VideoSvcApi.TITLE_PARAMETER) String title) {
        return videos.findByName(title);
    }

    @RequestMapping(value=VideoSvcApi.VIDEO_DURATION_SEARCH_PATH, method= RequestMethod.GET)
    public @ResponseBody Collection<Video> findByDurationLessThan(@RequestParam(VideoSvcApi.DURATION_PARAMETER) long maxduration) {
        return videos.findByDurationLessThan(maxduration);
    }

}
