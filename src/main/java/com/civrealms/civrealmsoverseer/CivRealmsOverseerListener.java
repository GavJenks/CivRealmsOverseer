package main.java.com.civrealms.civrealmsoverseer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class CivRealmsOverseerListener implements Listener {
    private CivRealmsOverseer plugin;
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
    public void onPurplePortal(PlayerPortalEvent event){
        //before this ever got called, a database was maintained over time of portal locations across servers, this avoids any need for call and response all at once to check situations right at portaling.
        //here, the person is trying to go in the portal. This whole thing should be async (not a big deal for portals you already stand there like 5 seconds)
        //check the database for whether both worlds have portals active.
        //also push a DB entry with the requested arrival location of this player. This is already async so just wait for it to finish to move on. No surprises when we arrive
        //possibly check again right now before actually sending them that their inventory checks out as the same as what was sent to avoid dupes.
        //if everything good, no more DB calls needed, immediately call the bungee send thing (see main class method here), and when they arrive, their intended landing zone will be on file
    }
  
    //then add another listener for login, which looks up the requested arrival zone as described above (don't care if the portal is unlit by now or whatever)
}


