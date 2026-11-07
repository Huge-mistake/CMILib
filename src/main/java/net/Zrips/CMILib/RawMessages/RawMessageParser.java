package net.Zrips.CMILib.RawMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.Zrips.CMILib.CMILib;
import net.Zrips.CMILib.Colors.CMIChatColor;
import net.Zrips.CMILib.Items.CMIItemStack;
import net.Zrips.CMILib.Logs.CMIDebug;
import net.Zrips.CMILib.Shadow.ShadowCommand;
import net.Zrips.CMILib.Shadow.ShadowCommandType;
import net.Zrips.CMILib.commands.CommandsHandler;

public class RawMessageParser {

    public static RawMessage translateRawMessage(CommandSender sender, String textLine) {
        return translateRawMessage(sender, textLine, false);
    }

    public static RawMessage translateTextOnlyRawMessage(String textLine) {

        textLine = RawMessage.supportsAdventureComponents() ? textLine : textLine.replace("\n", "\\n");

        List<String> split = new ArrayList<String>();
        if (textLine.contains("<Next>"))
            split.addAll(Arrays.asList(textLine.split("<Next>")));
        else
            split.add(textLine);

        RawMessage rm = new RawMessage();

        ArrayList<String> temp = new ArrayList<String>(split);
        split.clear();
        for (String one : temp) {
            if (one.split("<T>").length > 2) {
                for (String spone : one.split("<T>")) {
                    if (spone == null || spone.isEmpty())
                        continue;
                    split.add("<T>" + spone);
                }
            } else
                split.add(one);
        }

        for (String message : split) {
            processText(rm, message);
            processHover(rm, message);
        }

        return rm;
    }

    public static RawMessage translateRawMessage(CommandSender sender, String textLine, boolean book) {

        textLine = RawMessage.supportsAdventureComponents() ? textLine : textLine.replace("\n", "\\n");

        List<String> split = new ArrayList<String>();
        if (textLine.contains("<Next>"))
            split.addAll(Arrays.asList(textLine.split("<Next>")));
        else
            split.add(textLine);

        ArrayList<String> temp = new ArrayList<String>(split);
        split.clear();
        for (String one : temp) {
            if (one.split("<T>").length > 2) {
                for (String spone : one.split("<T>")) {
                    if (spone == null || spone.isEmpty())
                        continue;
                    split.add("<T>" + spone);
                }
            } else
                split.add(one);
        }

        RawMessage rm = new RawMessage();

        for (String message : split) {

            processText(rm, message);
            processHover(rm, message);
            processCommand(rm, message);

            if (message.contains("<ITEM>")) {
                String st = RawMessage.extractTag(message, "ITEM");
                CMIItemStack stack = null;
                if (st.equalsIgnoreCase("%itemInHand%")) {
                    if (sender instanceof Player)
                        stack = new CMIItemStack(CMIItemStack.getItemInMainHand((Player) sender));
                } else if (st.equalsIgnoreCase("%itemInOffHand%")) {
                    if (sender instanceof Player)
                        stack = new CMIItemStack(CMIItemStack.getItemInOffHand((Player) sender));
                } else
                    stack = CMILib.getInstance().getItemManager().getItem(st);

                if (stack != null)
                    rm.addItem(stack.getItemStack());
            }

            if (message.contains("<URL>")) {
                rm.addUrl(RawMessage.extractTag(message, "URL"));
            }

            String command = null;

            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (message.contains("<CC>")) {
                    String consoleCommand = RawMessage.extractTag(message, "CC");
                    String id = ShadowCommand.addShadowCmd(player, consoleCommand, false, ShadowCommandType.Console);
                    command = CommandsHandler.getLabel() + " shadowcmd " + id + " (" + consoleCommand + ")";
                    if (book)
                        command = consoleCommand;
                }
                if (message.contains("<CCI>")) {
                    String consoleCommand = RawMessage.extractTag(message, "CCI");
                    String id = ShadowCommand.addShadowCmd(player, consoleCommand, true, ShadowCommandType.Console);
                    command = CommandsHandler.getLabel() + " shadowcmd " + id + " (" + consoleCommand + ")";
                    if (book)
                        command = consoleCommand;
                }
            }

            if (message.contains("<SC>")) {
                rm.addSuggestion(RawMessage.extractTag(message, "SC"));
            }

            if (message.contains("<SI>")) {
                rm.addInsertion(RawMessage.extractTag(message, "SI"));
            }

            rm.addCommand(command);
        }

        return rm;
    }

    private static void processHover(RawMessage rm, String message) {
        if (!message.contains("<H>"))
            return;

        rm.addHover(RawMessage.extractTag(message, "H"));
    }

    private static void processCommand(RawMessage rm, String message) {
        if (!message.contains("<C>"))
            return;

        rm.addCommand(RawMessage.extractTag(message, "C"));
    }

    private static void processText(RawMessage rm, String message) {
        if (!message.contains("<T>"))
            return;

        String text = RawMessage.extractTag(message, "T");

        Matcher match = RawMessage.patern.matcher(text);

        if (match.find()) {
            String url = match.group();

            String potentialName = url.substring(url.lastIndexOf('.') + 1);

            Player player = potentialName.isEmpty() ? null : Bukkit.getPlayerExact("." + potentialName);

            // Bedrock users have a dot in their name, so we need to check if the player is
            // online first before we assume it's a url
            if (player != null) {
                rm.addText(text);
            } else {

                String[] sp = text.split(url);
                if (sp.length > 0) {
                    if (sp[0].endsWith("&")) {
                        rm.addText(CMIChatColor.translate(sp[0]) + url.substring(0, 1));
                        url = url.substring(1);
                    } else {
                        String t = CMIChatColor.translate(sp[0]);
                        if (!t.equals(url))
                            rm.addText(t);
                    }
                }

                rm.addText(url);
                rm.addUrl(url);
                if (sp.length > 1) {
                    rm.addText(sp[1]);
                }
            }
        } else {
            rm.addText(text);
        }
    }
}
