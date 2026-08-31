package com.blackjackcalculator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlackjackConfig {
    public enum Role { HOST, VIEWER }
    public record FrameSelection(String firstKey, String secondKey) {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "blackjackcalculator.json";
    private static int hostX = 8, hostY = 8, viewerX = -1, viewerY = 8;
    private static float hostScale = 1.0f, viewerScale = 1.0f;
    private static final Map<String, Role> FRAME_ROLES = new HashMap<>();
    private static final Map<String, Integer> HOST_SCANNED_ITEMS = new LinkedHashMap<>();
    private static final Map<String, Integer> VIEWER_SCANNED_ITEMS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Integer>> CONTAINER_SCANS = new HashMap<>();
    private static Role activeRole = Role.HOST;
    private static String hostFirstFrame, hostSecondFrame, viewerFirstFrame, viewerSecondFrame;
    private static boolean loaded;
    private BlackjackConfig() {}
    public static void load(MinecraftClient client) { if (loaded) return; loaded = true; Path path = client.runDirectory.toPath().resolve(FILE_NAME); if (!Files.exists(path)) return; try { JsonObject root = JsonParser.parseReader(Files.newBufferedReader(path)).getAsJsonObject(); hostX=root.has("hostX")?root.get("hostX").getAsInt():hostX; hostY=root.has("hostY")?root.get("hostY").getAsInt():hostY; viewerX=root.has("viewerX")?root.get("viewerX").getAsInt():viewerX; viewerY=root.has("viewerY")?root.get("viewerY").getAsInt():viewerY; hostScale=root.has("hostScale")?root.get("hostScale").getAsFloat():hostScale; viewerScale=root.has("viewerScale")?root.get("viewerScale").getAsFloat():viewerScale; if(root.has("activeRole"))try{activeRole=Role.valueOf(root.get("activeRole").getAsString());}catch(IllegalArgumentException ignored){} hostFirstFrame=getString(root,"hostFirstFrame"); hostSecondFrame=getString(root,"hostSecondFrame"); viewerFirstFrame=getString(root,"viewerFirstFrame"); viewerSecondFrame=getString(root,"viewerSecondFrame"); loadMap(root,"hostScannedItems",HOST_SCANNED_ITEMS); loadMap(root,"viewerScannedItems",VIEWER_SCANNED_ITEMS); if(root.has("frameRoles")&&root.get("frameRoles").isJsonObject())for(var e:root.getAsJsonObject("frameRoles").entrySet())try{FRAME_ROLES.put(e.getKey(),Role.valueOf(e.getValue().getAsString()));}catch(IllegalArgumentException ignored){} }catch(Exception ignored){} }
    private static String getString(JsonObject root,String key){return root.has(key)&&!root.get(key).isJsonNull()?root.get(key).getAsString():null;}
    private static void loadMap(JsonObject root,String key,Map<String,Integer> target){if(root.has(key)&&root.get(key).isJsonObject())root.getAsJsonObject(key).entrySet().forEach(e->{try{target.put(e.getKey(),e.getValue().getAsInt());}catch(Exception ignored){}});}
    public static void save(MinecraftClient client){if(client==null)return;JsonObject root=new JsonObject();root.addProperty("hostX",hostX);root.addProperty("hostY",hostY);root.addProperty("viewerX",viewerX);root.addProperty("viewerY",viewerY);root.addProperty("hostScale",hostScale);root.addProperty("viewerScale",viewerScale);root.addProperty("activeRole",activeRole.name());if(hostFirstFrame!=null)root.addProperty("hostFirstFrame",hostFirstFrame);if(hostSecondFrame!=null)root.addProperty("hostSecondFrame",hostSecondFrame);if(viewerFirstFrame!=null)root.addProperty("viewerFirstFrame",viewerFirstFrame);if(viewerSecondFrame!=null)root.addProperty("viewerSecondFrame",viewerSecondFrame);JsonObject roles=new JsonObject();FRAME_ROLES.forEach((k,v)->roles.addProperty(k,v.name()));root.add("frameRoles",roles);JsonObject host=new JsonObject();HOST_SCANNED_ITEMS.forEach(host::addProperty);root.add("hostScannedItems",host);JsonObject viewer=new JsonObject();VIEWER_SCANNED_ITEMS.forEach(viewer::addProperty);root.add("viewerScannedItems",viewer);try{Files.writeString(client.runDirectory.toPath().resolve(FILE_NAME),GSON.toJson(root));}catch(IOException ignored){}}
    public static String frameKey(ItemFrameEntity frame,RegistryKey<World> dimension){return dimension.getValue()+":"+frame.getUuid();}
    public static Role getRole(ItemFrameEntity frame,RegistryKey<World> dimension){return FRAME_ROLES.get(frameKey(frame,dimension));}
    public static void setRole(ItemFrameEntity frame,RegistryKey<World> dimension,Role role){FRAME_ROLES.put(frameKey(frame,dimension),role);}
    public static void clearRole(ItemFrameEntity frame,RegistryKey<World> dimension){FRAME_ROLES.remove(frameKey(frame,dimension));}
    public static void setFirstSelection(Role role,String key){if(role==Role.HOST)hostFirstFrame=key;else viewerFirstFrame=key;}
    public static void setSecondSelection(Role role,String key){if(role==Role.HOST)hostSecondFrame=key;else viewerSecondFrame=key;}
    public static FrameSelection getSelection(Role role){return role==Role.HOST?new FrameSelection(hostFirstFrame,hostSecondFrame):new FrameSelection(viewerFirstFrame,viewerSecondFrame);}
    public static void clearSelection(Role role){if(role==Role.HOST){hostFirstFrame=null;hostSecondFrame=null;}else{viewerFirstFrame=null;viewerSecondFrame=null;}}
    public static void saveContainerScan(String containerKey,Role role,Map<String,Integer> values){Map<String,Integer> copy=new LinkedHashMap<>(values);CONTAINER_SCANS.put(containerKey+"|"+role.name(),copy);Map<String,Integer> target=role==Role.HOST?HOST_SCANNED_ITEMS:VIEWER_SCANNED_ITEMS;target.clear();target.putAll(copy);}
    public static int getScannedValue(Role role,String signature){Integer value=(role==Role.HOST?HOST_SCANNED_ITEMS:VIEWER_SCANNED_ITEMS).get(signature);return value==null?-1:value;}
    public static String containerKey(RegistryKey<World> dimension,BlockPos pos){return dimension.getValue()+":"+pos.toShortString();}
    public static Role getActiveRole(){return activeRole;} public static void setActiveRole(Role role){activeRole=role;}
    public static float getHostScale(){return hostScale;} public static float getViewerScale(){return viewerScale;}
    public static void setHostScale(float scale){hostScale=Math.max(0.5f,Math.min(2.0f,scale));} public static void setViewerScale(float scale){viewerScale=Math.max(0.5f,Math.min(2.0f,scale));}
    public static int getHostX(){return hostX;} public static int getHostY(){return hostY;} public static int getViewerX(){return viewerX;} public static int getViewerY(){return viewerY;}
    public static void setHostPosition(int x,int y){hostX=x;hostY=y;} public static void setViewerPosition(int x,int y){viewerX=x;viewerY=y;}
    public static void resetPositions(){hostX=8;hostY=8;viewerX=-1;viewerY=8;hostScale=1.0f;viewerScale=1.0f;}
}
