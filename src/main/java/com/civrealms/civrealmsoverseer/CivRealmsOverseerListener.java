package main.java.com.civrealms.civrealmsoverseer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

/**
 * @author Crimeo
 */
public class CivRealmsOverseerListener implements Listener {
    private CivRealmsOverseer plugin;
    
    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
    public void onPurplePortal(PlayerPortalEvent event){
        //here, we will run an async method that checks the database for whether both worlds have portals active.
            //in that same async function, also push a DB entry with the requested arrival location of this player, so that we know that gets sent out before the answer is returned here
            //maybe even stick in a delay at the end before return statement?
        //if valid: now just directly call the bungee send thing (see main class method here), and when they arrive, their intended landing zone will be on file
    }
    
    //then add a listener for login, which looks up the requested arrival zone as described above.
}


