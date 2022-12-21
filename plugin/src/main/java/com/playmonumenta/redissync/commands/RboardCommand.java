package com.playmonumenta.redissync.commands;

import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RBoardAPI;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RboardCommand {
	static final String COMMAND = "rboard";
	static final CommandPermission PERMS = CommandPermission.fromString("monumenta.command.rboard");

	@FunctionalInterface
	public interface RboardAction {
		void run(CommandSender sender, Object[] args, String rboardName, String scoreboardName) throws Exception;
	}

	@SuppressWarnings("unchecked")
	private static void regWrapper(List<Argument<?>> arguments, RboardAction exec) {
		CommandExecutor cmdExec = (sender, args) -> {
			try {
				if (args[0] instanceof Collection<?>) {
					for (Player player : (Collection<Player>)args[0]) {
						exec.run(sender, args, player.getUniqueId().toString(), player.getName());
					}
				} else {
					if (!((String)args[0]).startsWith("$")) {
						throw CommandAPI.failWithString("Fakeplayer names must start with a $");
					} else {
						exec.run(sender, args, (String)args[0], (String)args[0]);
					}
				}
			} catch (WrapperCommandSyntaxException ex) {
				throw ex;
			} catch (Exception ex) {
				throw CommandAPI.failWithString(ex.getMessage());
			}
		};

		/* Replace the players argument with a simple string for fakeplayers */
		List<Argument<?>> fakePlayerArguments = new ArrayList<>(40);
		for (Argument<?> arg : arguments) {
			if (arg.getNodeName().equals("players")) {
				fakePlayerArguments.add(new TextArgument("name"));
			} else {
				fakePlayerArguments.add(arg);
			}
		}

		/* First register a variant with fakeplayers as a string argument, replacing the "players" arg
		 * This ordering is apparently important
		 */
		new CommandAPICommand(COMMAND)
			.withArguments(fakePlayerArguments)
			.withPermission(PERMS)
			.executes(cmdExec)
			.register();

		/* Second one of these registers as-is, with 'players' being a collection of players */
		new CommandAPICommand(COMMAND)
			.withArguments(arguments)
			.withPermission(PERMS)
			.executes(cmdExec)
			.register();
	}

	public static void register(Plugin plugin) {
		List<Argument<?>> arguments = new ArrayList<>(40);

		/********************* Set *********************/
		RboardAction action = (sender, args, rboardName, scoreboardName) -> {
			Map<String, String> values = new LinkedHashMap<>();
			for (int i = 1; i < args.length; i += 2) {
				values.put((String)args[i], Integer.toString((Integer)args[i + 1]));
			}
			RBoardAPI.set(rboardName, values);
		};

		arguments.add(new LiteralArgument("set"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		for (int i = 0; i < 6; i++) {
			arguments.add(new ObjectiveArgument("objective" + i));
			arguments.add(new IntegerArgument("value" + i));
			regWrapper(arguments, action);
		}

		/********************* Store *********************/
		action = (sender, args, rboardName, scoreboardName) -> {
			Map<String, String> values = new LinkedHashMap<>();
			for (int i = 1; i < args.length; i += 1) {
				values.put((String)args[i], Integer.toString(ScoreboardUtils.getScoreboardValue(scoreboardName, (String)args[i])));
			}
			RBoardAPI.set(rboardName, values);
		};

		arguments.clear();
		arguments.add(new LiteralArgument("store"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		for (int i = 0; i < 6; i++) {
			arguments.add(new ObjectiveArgument("objective" + i));
			regWrapper(arguments, action);
		}

		/********************* Add *********************/
		arguments.clear();
		arguments.add(new LiteralArgument("add"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		arguments.add(new ObjectiveArgument("objective"));
		arguments.add(new IntegerArgument("value"));
		regWrapper(arguments, (sender, args, rboardName, scoreboardName) ->
			RBoardAPI.add(rboardName, (String)args[1], (Integer)args[2]));

		/********************* AddScore *********************/
		arguments.clear();
		arguments.add(new LiteralArgument("addscore"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		arguments.add(new ObjectiveArgument("objective"));
		arguments.add(new ObjectiveArgument("objectiveToAdd"));
		regWrapper(arguments, (sender, args, rboardName, scoreboardName) ->
			RBoardAPI.add(rboardName, (String)args[1], ScoreboardUtils.getScoreboardValue(scoreboardName, (String)args[2])));

		/********************* Reset *********************/
		action = (sender, args, rboardName, scoreboardName) -> {
			String[] values = new String[args.length - 1];
			for (int i = 1; i < args.length; i += 1) {
				values[i - 1] = (String)args[i];
			}
			RBoardAPI.reset(rboardName, values);
		};

		arguments.clear();
		arguments.add(new LiteralArgument("reset"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		for (int i = 0; i < 6; i++) {
			arguments.add(new ObjectiveArgument("objective" + i));
			regWrapper(arguments, action);
		}

		/********************* ResetAll *********************/
		action = (sender, args, rboardName, scoreboardName) ->
			RBoardAPI.resetAll(rboardName);

		arguments.clear();
		arguments.add(new LiteralArgument("resetall"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		regWrapper(arguments, action);

		/********************* GetAll *********************/
		action = (sender, args, rboardName, scoreboardName) -> {
			MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin,
			                                                  RBoardAPI.getAll(rboardName),
			                                                  (Map<String, String> data, Throwable except) -> {
				if (except != null) {
					plugin.getLogger().severe("rboard getall failed:" + except.getMessage());
					except.printStackTrace();
				} else {
					StringBuilder output = new StringBuilder("[");
					boolean first = true;
					for (Map.Entry<String, String> entry : data.entrySet()) {
						if (!first) {
							output.append(" ");
						}
						output.append(ChatColor.GOLD).append(entry.getKey()).append(ChatColor.WHITE).append("=").append(ChatColor.GREEN).append(entry.getValue());
						first = false;
					}
					output.append(ChatColor.WHITE).append("]");
					sender.sendMessage(output.toString());
				}
			});
		};

		arguments.clear();
		arguments.add(new LiteralArgument("getall"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		regWrapper(arguments, action);

		/********************* Get *********************/
		action = (sender, args, rboardName, scoreboardName) -> {
			String[] objects = new String[args.length - 2];
			for (int j = 2; j < args.length; j += 1) {
				objects[j - 2] = (String)args[j];
			}
			MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin,
			                                                  RBoardAPI.get(rboardName, objects),
			                                                  (Map<String, String> data, Throwable except) -> {
				if (except != null) {
					plugin.getLogger().severe("rboard get failed:" + except.getMessage());
					except.printStackTrace();
				} else {
					for (Map.Entry<String, String> entry : data.entrySet()) {
						ScoreboardUtils.setScoreboardValue(scoreboardName, entry.getKey(), Integer.parseInt(entry.getValue()));
					}
					for (FunctionWrapper func : (FunctionWrapper[]) args[1]) {
						func.run();
					}
				}
			});
		};

		arguments.clear();
		arguments.add(new LiteralArgument("get"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		arguments.add(new FunctionArgument("function"));
		for (int i = 0; i < 15; i++) {
			arguments.add(new ObjectiveArgument("objective" + i));
			regWrapper(arguments, action);
		}

		/********************* AddAndGet *********************/
		action = (sender, args, rboardName, scoreboardName) -> {
			MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin,
			                                                  RBoardAPI.add(rboardName, (String)args[2], (Integer)args[3]),
			                                                  (Long data, Throwable except) -> {
				if (except != null) {
					plugin.getLogger().severe("rboard addandget failed:" + except.getMessage());
					except.printStackTrace();
				} else {
					ScoreboardUtils.setScoreboardValue(scoreboardName, (String)args[2], data.intValue());
					for (FunctionWrapper func : (FunctionWrapper[]) args[1]) {
						func.run();
					}
				}
			});
		};

		arguments.clear();
		arguments.add(new LiteralArgument("addandget"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		arguments.add(new FunctionArgument("function"));
		arguments.add(new ObjectiveArgument("objective"));
		arguments.add(new IntegerArgument("value"));
		regWrapper(arguments, action);

		/********************* GetAndReset *********************/
		action = (sender, args, rboardName, scoreboardName) -> {
			String[] objects = new String[args.length - 2];
			for (int j = 2; j < args.length; j += 1) {
				objects[j - 2] = (String)args[j];
			}
			MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin,
			                                                  RBoardAPI.getAndReset(rboardName, objects),
			                                                  (Map<String, String> data, Throwable except) -> {
				if (except != null) {
					plugin.getLogger().severe("rboard getandreset failed:" + except.getMessage());
					except.printStackTrace();
				} else {
					for (Map.Entry<String, String> entry : data.entrySet()) {
						ScoreboardUtils.setScoreboardValue(scoreboardName, entry.getKey(), Integer.parseInt(entry.getValue()));
					}
					for (FunctionWrapper func : (FunctionWrapper[]) args[1]) {
						func.run();
					}
				}
			});
		};

		arguments.clear();
		arguments.add(new LiteralArgument("getandreset"));
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		arguments.add(new FunctionArgument("function"));
		for (int i = 0; i < 6; i++) {
			arguments.add(new ObjectiveArgument("objective" + i));
			regWrapper(arguments, action);
		}
	}
}
