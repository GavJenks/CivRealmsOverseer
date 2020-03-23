package main.java.com.civrealms.civrealmsoverseer;

//java google etc
import java.io.IOException;
import java.sql.Connection;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.io.DataInputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

//bukkit
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class CivRealmsOverseer
        extends JavaPlugin
        implements Listener, PluginMessageListener {

    FileConfiguration config = getConfig();

    private Connection connection;
    private String host;
    private String database;
    private String username;
    private String password;
    private int port;
    private String worldName;
    public HashMap<Location,ArrayList<String>> portalLedger = new HashMap<Location,ArrayList<String>>(); //Location is a location, String list is a list of worlds that have that location with a portal lit.
    
    public static Logger LOG = Logger.getLogger("CivRealmChat");
    
    public void onEnable() {

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new CivRealmsOverseerListener(), this);

        //init DB connection
        this.host = this.config.getString("hostname");
        this.port = this.config.getInt("port");
        this.database = this.config.getString("database");
        this.username = this.config.getString("username");
        this.password = this.config.getString("password");
        this.worldName = this.config.getString("worldName");
        
        //load up known prison portal locations
        loadPrisonPortals();
        
        //create player routing table if none. This is where people should show up and with what inventory when they login next.
        initPlayerRoutingTable();
    }
    
    //This just actually sends them there via bungee, it does not (yet at least, might be a good idea) properly log their arrival coords desired and their inventory on arrival
    //(i.e. eventually it should have a CRO_tp_announce built in?)
    public void sendPlayerSomewhere(String playerToSendUUID, String targetServerName){
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
        out.writeUTF("Connect");
        out.writeUTF(targetServerName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bukkit.getPlayer(playerToSendUUID).sendPluginMessage(this, "BungeeCord", b.toByteArray());
    }
    

    @Override
    public void onPluginMessageReceived(String channel, Player recipient, byte[] message) {
        //FORMAT FOR ALL INCOMING messages from other servers:
            //Common format no matter what: "BungeeCord" then [subchannel "Forward" and its various arguments] 
            //then [plugin name "CivRealmOverseer"] then [command name like "CRO_tp_announce" for example]
            //then payload: just everything else, depends on the command what is what here
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String firstField = in.readUTF();
        if (!firstField.equals("CivRealmOverseer")) {
            return;
        }
        
        //LOG.info("[debug] CROverseer found a plugin message intended for it.");
        short len = in.readShort();
        byte[] msgbytes = new byte[len];
        in.readFully(msgbytes);
        DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
        try {
            //More stuff common to all incoming messages:
            String command = msgin.readUTF();
            LOG.info("(debug) [CROverseer] command received, type: " + command);
            
            //###############
            //If or switch statement or whatever, checking which "command" it is, an then doing various
            //msgin.readInt(), msgin.readUTF() etc. etc. as appropriate to that command.
            //###############
            
            /*if (command.equals("CRO_tp_announce")){ //one server telling another the location the person should show up when they arrive any moment now
                //Somebody is about to come to a world. Sender is the person, message contains their UUID, target world, X,Y,Z of the block for them to stand on, and inventory.
                try {
                    String name = msgin.readUTF();
                    UUID uuid = UUID.fromString(msgin.readUTF());
                    String targetWorld = msgin.readUTF();
                    if (targetWorld != worldName){
                        return;
                    }
                    int x = msgin.readInt();
                    int y = msgin.readInt();
                    int z = msgin.readInt();
                    String inventoryString = msgin.readUTF();
                    
                    //######### inventory parsing stuff, needs to be redone better 
                    //and actually store the results in memory for when they arrive, blah blah
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else */if (command.equals("ErrorFeedback")) {
                //A message that just dumps raw error text to display to a particular user for all sorts of cross-world feedback about checks that failed, etc.
                //not actually needed yet for anything.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //############## Various possible commands as separate methods like the following one, since they all have totally different parameters needed:
    
    //for telling another server that this player will be arriving, and this is where we want them to go and with what inventory:
    public ByteArrayOutputStream msgPrep_CRO_tp_announce(Player playerToTP, int x, int y, int z, String targetWorld){
        ByteArrayOutputStream msgbytes = new ByteArrayOutputStream(); //these two lines just at the top of every command prep, rest of junk in "msgSend" generic method
        DataOutputStream msgout = new DataOutputStream(msgbytes);
        try{
            msgout.writeUTF("CRO_tp_announce");
            msgout.writeUTF(playerToTP.getDisplayName());
            msgout.writeUTF(playerToTP.getUniqueId().toString());
            msgout.writeUTF(targetWorld);
            msgout.writeInt(x);
            msgout.writeInt(y);
            msgout.writeInt(z);
            msgout.writeUTF(serializeInventory(playerToTP));
        } catch(IOException e){e.printStackTrace();}
        return msgbytes;
    }
    
    //assuming for now we won't ever have a reason to send to another plugin than this, but just in case, two methods:
    public void msgSend(Player sender, String command, ByteArrayOutputStream payload){
        msgSend(sender, command, payload, "CivRealmOverseer");
    }
    
    public void msgSend(Player sender, String command, ByteArrayOutputStream payload, String intendedPlugin){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(intendedPlugin);
        out.writeShort(payload.toByteArray().length);
        out.write(payload.toByteArray());
        sender.sendPluginMessage(this, "BungeeCord", out.toByteArray());
        LOG.info("[CROverseer]: sent pluginMessage, command: " + command + ", payload: " + out.toString());
    }
    
    public String serializeInventory(Player p){
        //######### TO DO
        return null;
    }
    
    public void openConnection() {
        try {
            if (this.connection != null && !this.connection.isClosed()) {
                return;
            }
            synchronized (this) {
                if (this.connection != null && !this.connection.isClosed()) {
                    return;
                }
                Class.forName("com.mysql.jdbc.Driver");
                this.connection = DriverManager.getConnection(
                        "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database, this.username, this.password);
            }
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void loadPrisonPortals(){
        try{
            openConnection();
            Statement statement = this.connection.createStatement();
            statement.executeUpdate("create table if not exists `prison_portals` (`x` INT(8), `y` INT(8), `z` INT(8),`world` varchar(30));");
            ResultSet result = statement.executeQuery("select * from prison_portals;");
            World w;
            while (result.next()) {
                int x = result.getInt(1);
                int y = result.getInt(2);
                int z = result.getInt(3);
                String world = result.getString(4);
                w = Bukkit.getWorld(world);
                Location loc = new Location(w,x,y,z);
                ArrayList<String> worldList = new ArrayList<String>();
                if (portalLedger.containsKey(loc)){
                    worldList = portalLedger.get(loc);
                    if(!worldList.contains(world)){
                        worldList.add(world);
                    }
                    portalLedger.put(loc, worldList);
                } else {
                    worldList.add(world);
                    portalLedger.put(loc,worldList);
                }
            }
            if (!statement.isClosed()){statement.close();};
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    
    public void loadJoiningPlayerDestination(UUID joiningUUID){
        try{
            openConnection();
            Statement statement = this.connection.createStatement();
            ResultSet result = statement.executeQuery("select * from overseer_player_routing where uuid = " + joiningUUID + ";");
            result.next();
            UUID uuid = UUID.fromString(result.getString(1));
            int x = result.getInt(2);
            int y = result.getInt(3);
            int z = result.getInt(4);
            String targetWorld = result.getString(5);
            String inventoryString = result.getString(6);
            
            //########## DO STUFF / RETURN a "destination" ackage object / WHATEVER
            
            if (!statement.isClosed()){statement.close();};
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    
    //no overall save command, only update on the fly as portals are lit or extinguished:
    public void updatePortal(Connection connection, ArrayList<Location> locationsToUpdate, boolean addingPortals){ //addingPortals = true to build up the DB, false to remove this list of portals from the DB instead.
        BukkitRunnable r = new BukkitRunnable() {
            public void run() {
                try{
                    openConnection();
                    Statement statement = connection.createStatement();
                    String query = "SELECT * FROM prison_portals WHERE world = " + worldName + " and (";
                    String queryAppend = "";
                    ResultSet result;
                    for (Location loc : locationsToUpdate){
                        int x = loc.getBlockX();
                        int y = loc.getBlockY();
                        int z = loc.getBlockZ();
                        queryAppend = "(x = " + x + " and y = " + y + " and z = " + z + ") or ";
                        query = query + queryAppend;
                    }
                    if(query.endsWith(" or ")){
                        query = query.substring(0,query.length()-4); //chop off the last " or "
                        query = query + ");"; //parenthesis is because we had "where world = world AND ([long string of other stuff]);" with that close parens at the end.
                        result = statement.executeQuery(query); //only the rows already existing, if any, for the list of locations given
                    } else {
                        result = null;
                    }
                    
                    //So now we have a result set of all DB entries that DO exist for this portal, or null if none.
                    if (addingPortals){
                        //go through result set and remove those from the arraylist, then INSERT any remaining
                        if (result != null){
                            while (result.next()) {
                                int x = result.getInt(1);
                                int y = result.getInt(2);
                                int z = result.getInt(3);
                                String world = result.getString(4);
                                locationsToUpdate.remove(new Location(Bukkit.getWorld(world),x,y,z)); //this location is already in DB, so don't add it.
                            }
                        }
                        //remaining ones still left: add them all to DB in one batch:
                        String insertText = "INSERT INTO prison_portals VALUES ";
                        for (Location loc : locationsToUpdate){
                            insertText = insertText + "(" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," + worldName + "),";
                        }
                        if(insertText.endsWith(",")){
                            insertText = insertText.substring(0,insertText.length()-1); //chop off the last ","
                            insertText = insertText + ";";
                            statement.executeUpdate(insertText);
                        } 
                        
                    } else { //else, removing portals:
                        //go through result set and immediately add those to the batch to remove, then do it.
                        String deleteText = "DELETE FROM prison_portals WHERE ";
                        if (result != null){
                            while (result.next()) {
                                int x = result.getInt(1);
                                int y = result.getInt(2);
                                int z = result.getInt(3);
                                String world = result.getString(4);
                                deleteText = deleteText + "(x = " + x + " and y = " + y + " and z = " + z + "and world = " + world + ") or ";
                            }
                        }
                        if(deleteText.endsWith(" or ")){
                            deleteText = deleteText.substring(0,deleteText.length()-4); //chop off the last " or "
                            deleteText = deleteText + ";";
                            statement.executeUpdate(deleteText);
                        } 
                        //else... if null, nothing remains to be done.
                    }
                    if (!statement.isClosed()){statement.close();};
                } catch (SQLException e){
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(this);
    }
        
    public Connection getConnection(){
        return connection;
    }
    
    public void initPlayerRoutingTable(){
        try{
            openConnection();
            Statement statement = this.connection.createStatement();
            statement.executeUpdate("create table if not exists `overseer_player_routing` (`uuid` VARCHAR(100) PRIMARY KEY, `target_x` INT(10), `target_y` INT(10), `target_z` INT(10),`target_world` VARCHAR(100)), `inventory` VARCHAR(max);");
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
}
