package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import retrofit.http.Streaming;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by michal on 8/16/14.
 */

@Controller
public class VideoSvc   {
    public static final String DATA_PARAMETER = "data";

    public static final String ID_PARAMETER = "id";

    public static final String VIDEO_SVC_PATH = "/video";

    public static final String VIDEO_DATA_PATH = VIDEO_SVC_PATH + "/{id}/data";


    // An in-memory list that the servlet uses to store the
    // videos that are sent to it by clients
    private Map<Long, Video> videos = new ConcurrentHashMap<Long, Video>();

    private static final AtomicLong currentId = new AtomicLong(0L);

    @ResponseStatus(value = org.springframework.http.HttpStatus.NOT_FOUND)
    private final class ResourceNotFoundException extends RuntimeException {
        //  class definition
    }

    private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }

    private void setDataUrl(Video entity) {
        entity.setDataUrl(getDataUrl(entity.getId()));
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }

    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://"+request.getServerName()
                        + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
    }

//    @Override
    @RequestMapping(value=VIDEO_SVC_PATH, method= RequestMethod.GET)
    public @ResponseBody Collection<Video> getVideoList() {
        return videos.values();
    }

//    @Override
    @RequestMapping(value=VIDEO_SVC_PATH, method=RequestMethod.POST)
    public @ResponseBody Video addVideo(@RequestBody Video v) {
        checkAndSetId(v);
        setDataUrl(v);
        videos.put(v.getId(), v);
        return v;
    }

//    @Override
//    @Multipart
    @RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.POST)
    public @ResponseBody VideoStatus setVideoData(@PathVariable(ID_PARAMETER) long id, @RequestParam(DATA_PARAMETER) Part videoData) {
        Video video  = videos.get(id);
        if (video == null) {
            throw new ResourceNotFoundException();
        }

        VideoFileManager videoFileManager;
        try {
            videoFileManager = VideoFileManager.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            videoFileManager.saveVideoData(video, videoData.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new VideoStatus(VideoStatus.VideoState.READY);
    }

//    @Override
    @Streaming
    @RequestMapping(value=VIDEO_DATA_PATH, method=RequestMethod.GET)
    public void getData(@PathVariable(ID_PARAMETER) long id, HttpServletResponse response) {
        Video video  = videos.get(id);
        VideoFileManager videoFileManager;
        try {
            videoFileManager = VideoFileManager.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (video == null || !videoFileManager.hasVideoData(video) ) {
            throw new ResourceNotFoundException();
        }


        try {
            videoFileManager.copyVideoData(video, response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        return null;

    }

}

