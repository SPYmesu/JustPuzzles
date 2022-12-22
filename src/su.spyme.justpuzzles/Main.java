package su.spyme.justpuzzles;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Main extends JavaPlugin implements Listener{

    Economy econ;
    Map<String, String> questions = new HashMap<>();
    boolean activePuzzle;
    boolean puzzleType;
    int[] mathPuzzle;
    String textPuzzle;

    BukkitScheduler task;

    public void onEnable(){
        boolean vault = setupEconomy();
        if(!vault || econ == null){
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Vault hook failed");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "JustPuzzles v3.0 by SPY_me enabled.");
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        saveDefaultConfig();
        List<String> list = getConfig().getStringList("questions");
        for(String s : list){
            String[] split = s.split("/");
            questions.put(split[0], split[1]);
        }
        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if(activePuzzle){
                if(puzzleType){
                    if(textPuzzle == null){
                        return;
                    }
                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(getMessage("guess_text").replace("%puzzle%", textPuzzle));
                    Bukkit.broadcastMessage(" ");
                }else{
                    if(mathPuzzle == null){
                        return;
                    }
                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(getMessage("guess_math").replace("%first%", String.valueOf(mathPuzzle[0])).replace("%second%", String.valueOf(mathPuzzle[1])));
                    Bukkit.broadcastMessage(" ");
                }
            }else{
                puzzleType = new Random().nextBoolean();
                if(puzzleType){
                    List<String> questionsList = new ArrayList<>(questions.keySet());
                    textPuzzle = questions.get(questionsList.get(new Random().nextInt(questionsList.size())));
                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(getMessage("guess_text").replace("%puzzle%", textPuzzle));
                }else{
                    int int1 = new Random().nextInt(999);
                    int int2 = new Random().nextInt(999);
                    int int3 = int1 + int2;
                    mathPuzzle = new int[]{int1, int2, int3};
                    Bukkit.broadcastMessage(" ");
                    Bukkit.broadcastMessage(getMessage("guess_math").replace("%first%", String.valueOf(int1)).replace("%second%", String.valueOf(int2)));
                }
                Bukkit.broadcastMessage(" ");
                activePuzzle = true;
            }
        }, 0L, 1800L);
    }

    public void onDisable(){
        Bukkit.getPluginManager().disablePlugin(this);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "JustPuzzles v3.0 by SPY_me disabled.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args){
        if(command.getName().equalsIgnoreCase("ask")){
            if(args.length == 1 && args[0].equalsIgnoreCase("reload")){
                if(!commandSender.hasPermission("justpuzzles.reload")){
                    commandSender.sendMessage(getMessage("no_perm"));
                    return false;
                }
                activePuzzle = false;
                textPuzzle = null;
                mathPuzzle = null;
                saveDefaultConfig();
                reloadConfig();
                List<String> list = getConfig().getStringList("questions");
                for(String s : list){
                    String[] split = s.split("/");
                    questions.put(split[0], split[1]);
                }
                commandSender.sendMessage(getMessage("config_reloaded"));
            }
            if(args.length == 0){
                if(!(commandSender instanceof Player player)){
                    commandSender.sendMessage("Console is not allowed!");
                    return false;
                }
                if(!activePuzzle){
                    player.sendMessage(getMessage("no_puzzle"));
                    return false;
                }
                if(puzzleType){
                    if(textPuzzle == null){
                        return false;
                    }
                    player.sendMessage(getMessage("repeat"));
                    player.sendMessage(getMessage("guess_text").replace("%puzzle%", textPuzzle));
                }else{
                    if(this.mathPuzzle == null){
                        return false;
                    }
                    player.sendMessage(getMessage("repeat"));
                    player.sendMessage(getMessage("guess_math").replace("%first%", String.valueOf(mathPuzzle[0])).replace("%second%", String.valueOf(mathPuzzle[1])));
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onChat(final AsyncPlayerChatEvent event){
        if(!activePuzzle){
            return;
        }
        Player player = event.getPlayer();
        if(puzzleType){
            if(textPuzzle == null){
                return;
            }
            String answer = "";
            for(Map.Entry<String, String> entry : questions.entrySet()){
                if(entry.getValue().equals(textPuzzle)){
                    answer = entry.getKey();
                }
            }
            if(event.getMessage().equalsIgnoreCase(answer)){
                event.setCancelled(true);
                Bukkit.broadcastMessage(getMessage("correct_broadcast").replace("%player%", player.getDisplayName()));
                event.getPlayer().sendMessage(getMessage("correct"));
                addMoney(player, getConfig().getDouble("settings.REWARD", 50));
                textPuzzle = null;
                activePuzzle = false;
            }
        }else{
            if(!event.getMessage().matches("[0-9]+")){
                return;
            }
            if(mathPuzzle == null){
                return;
            }
            int answer = Integer.parseInt(event.getMessage());
            if(answer == mathPuzzle[2]){
                event.setCancelled(true);
                Bukkit.broadcastMessage(getMessage("correct_broadcast").replace("%player%", player.getDisplayName()));
                event.getPlayer().sendMessage(getMessage("correct"));
                addMoney(player, getConfig().getDouble("settings.REWARD", 50));
                mathPuzzle = null;
                activePuzzle = false;
            }
        }
    }

    public String getMessage(String key){
        return getConfig().getString("msg." + key.toUpperCase(), key).replaceAll("&", "§");
    }

    private boolean setupEconomy(){
        if(getServer().getPluginManager().getPlugin("Vault") == null){
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp == null){
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    private void addMoney(Player player, double count){
        EconomyResponse economyResponse = econ.depositPlayer(player, count);
        economyResponse.transactionSuccess();
    }

}