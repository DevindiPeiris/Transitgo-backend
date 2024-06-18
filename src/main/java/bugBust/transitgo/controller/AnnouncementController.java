package bugBust.transitgo.controller;

import java.security.Principal;
import java.util.Optional;
import java.util.logging.Logger;

import bugBust.transitgo.exception.AnnouncementNotFoundException;
import bugBust.transitgo.exception.UnauthorizedException;
import bugBust.transitgo.model.ActivityLog;
import bugBust.transitgo.model.Announcement;
import bugBust.transitgo.model.Role;
import bugBust.transitgo.model.User;
import bugBust.transitgo.repository.ActivityLogRepository;
import bugBust.transitgo.repository.AnnouncementRepository;
import bugBust.transitgo.services.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin("http://localhost:3000")
public class AnnouncementController {
    @Autowired
    private AnnouncementRepository announcementRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;


    @PostMapping("/announcement")
    Announcement newAnnouncement(@RequestBody Announcement newAnnouncement, Principal principal){
        newAnnouncement.setCreatedBy(principal.getName());
        Announcement savedAnnouncement  = announcementRepository.save(newAnnouncement);

        //Log Activity
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = ((User) userDetails).getId();
        activityLogService.logActivity(userId, "Announcement", savedAnnouncement.getDetails(), savedAnnouncement.getId());

        return savedAnnouncement;
    }

    @GetMapping("/announcements")
    List<Announcement> getAllAnnouncements(){
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/announcement/{id}")
    Announcement getAnnouncementById(@PathVariable Long id){
        return announcementRepository.findById(id)
                .orElseThrow(()->new AnnouncementNotFoundException(id));
    }

    @DeleteMapping("announcement/{id}")
    String deleteAnnouncement(@PathVariable Long id,Principal principal){
//        if(!announcementRepository.existsById(id)){
//            throw  new AnnouncementNotFoundException(id);
//        }
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
        if (!isAuthorizedToModify(principal, announcement)){
            throw new UnauthorizedException();
        }

        announcementRepository.deleteById(id);
        activityLogService.deleteActivityByActivityId(id);
        return "Announcement deleted successfully.";
    }

    @PutMapping("/announcement/{id}")
    Announcement updateAnnouncement(@RequestBody Announcement newAnnouncement, @PathVariable Long id){
        return  announcementRepository.findById(id)
                .map(announcement -> {
                    announcement.setDetails(newAnnouncement.getDetails());
                    
                    //update activity log
                    activityLogRepository.findByActivityId(id).ifPresent(activityLog -> {
                        activityLog.setDescription(newAnnouncement.getDetails());
                        activityLogRepository.save(activityLog);
                    });
                    return announcementRepository.save(announcement);
                }).orElseThrow(()-> new AnnouncementNotFoundException(id));
    }

    private boolean isAuthorizedToModify(Principal principal, Announcement announcement){
        String username = principal.getName();
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
         return announcement.getCreatedBy().equals(username) || userDetails.getAuthorities().contains(new SimpleGrantedAuthority(Role.admin.name()));
    }
}
