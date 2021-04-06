package dev.ftb.mods.ftbquests.gui;

import dev.ftb.mods.ftbguilibrary.config.ConfigGroup;
import dev.ftb.mods.ftbguilibrary.config.DoubleConfig;
import dev.ftb.mods.ftbguilibrary.config.gui.EditConfigFromStringScreen;
import dev.ftb.mods.ftbguilibrary.config.gui.EditConfigScreen;
import dev.ftb.mods.ftbguilibrary.misc.ButtonListBaseScreen;
import dev.ftb.mods.ftbguilibrary.utils.MouseButton;
import dev.ftb.mods.ftbguilibrary.utils.TooltipList;
import dev.ftb.mods.ftbguilibrary.widget.ContextMenuItem;
import dev.ftb.mods.ftbguilibrary.widget.GuiIcons;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbguilibrary.widget.SimpleTextButton;
import dev.ftb.mods.ftbguilibrary.widget.Theme;
import dev.ftb.mods.ftbguilibrary.widget.VerticalSpaceWidget;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class EditRewardTableScreen extends ButtonListBaseScreen {
	private class RewardTableSettingsButton extends SimpleTextButton {
		private RewardTableSettingsButton(Panel panel) {
			super(panel, new TranslatableComponent("gui.settings"), GuiIcons.SETTINGS);
			setHeight(12);
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			ConfigGroup group = new ConfigGroup(FTBQuests.MOD_ID);
			rewardTable.getConfig(rewardTable.createSubGroup(group));
			group.savedCallback = accepted -> run();
			new EditConfigScreen(group).openGui();
		}
	}

	private class SaveRewardTableButton extends SimpleTextButton {
		private SaveRewardTableButton(Panel panel) {
			super(panel, new TranslatableComponent("gui.accept"), GuiIcons.ACCEPT);
			setHeight(12);
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			closeGui();
			CompoundTag nbt = new CompoundTag();
			rewardTable.writeData(nbt);
			originalTable.readData(nbt);
			callback.run();
		}
	}

	private class AddWeightedRewardButton extends SimpleTextButton {
		private AddWeightedRewardButton(Panel panel) {
			super(panel, new TranslatableComponent("gui.add"), GuiIcons.ADD);
			setHeight(12);
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			List<ContextMenuItem> contextMenu = new ArrayList<>();

			for (RewardType type : RewardTypes.TYPES.values()) {
				if (!type.getExcludeFromListRewards()) {
					contextMenu.add(new ContextMenuItem(type.getDisplayName(), type.getIcon(), () -> {
						playClickSound();
						type.getGuiProvider().openCreationGui(this, rewardTable.fakeQuest, reward -> {
							rewardTable.rewards.add(new WeightedReward(reward, 1));
							openGui();
						});
					}));
				}
			}

			getGui().openContextMenu(contextMenu);
		}
	}

	private class WeightedRewardButton extends SimpleTextButton {
		private final WeightedReward reward;

		private WeightedRewardButton(Panel panel, WeightedReward r) {
			super(panel, r.reward.getTitle(), r.reward.getIcon());
			reward = r;
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			super.addMouseOverText(list);
			reward.reward.addMouseOverText(list);
			list.add(new TranslatableComponent("ftbquests.reward_table.weight").append(": " + reward.weight).append(new TextComponent(" [" + WeightedReward.chanceString(reward.weight, rewardTable.getTotalWeight(true)) + "]").withStyle(ChatFormatting.DARK_GRAY)));
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			List<ContextMenuItem> contextMenu = new ArrayList<>();
			contextMenu.add(new ContextMenuItem(new TranslatableComponent("selectServer.edit"), GuiIcons.SETTINGS, () -> {
				ConfigGroup group = new ConfigGroup(FTBQuests.MOD_ID);
				reward.reward.getConfig(reward.reward.createSubGroup(group));
				group.savedCallback = accepted -> run();
				new EditConfigScreen(group).openGui();
			}));

			contextMenu.add(new ContextMenuItem(new TranslatableComponent("ftbquests.reward_table.set_weight"), GuiIcons.SETTINGS, () -> {
				DoubleConfig c = new DoubleConfig(0D, Double.POSITIVE_INFINITY);
				EditConfigFromStringScreen.open(c, (double) reward.weight, 1D, accepted -> {
					if (accepted) {
						reward.weight = c.value.intValue();

						if (c.value < 1D) {
							for (WeightedReward reward : rewardTable.rewards) {
								reward.weight = (int) (reward.weight / c.value);
							}

							reward.weight = 1;
						}
					}

					run();
				});
			}));

			contextMenu.add(new ContextMenuItem(new TranslatableComponent("selectServer.delete"), GuiIcons.REMOVE, () -> {
				rewardTable.rewards.remove(reward);
				EditRewardTableScreen.this.refreshWidgets();
			}).setYesNo(new TranslatableComponent("delete_item", reward.reward.getTitle())));
			EditRewardTableScreen.this.openContextMenu(contextMenu);
		}

		@Override
		@Nullable
		public Object getIngredientUnderMouse() {
			return reward.reward.getIngredient();
		}
	}

	private final RewardTable originalTable;
	private final RewardTable rewardTable;
	private final Runnable callback;

	public EditRewardTableScreen(RewardTable r, Runnable c) {
		originalTable = r;
		rewardTable = new RewardTable(originalTable.file);
		CompoundTag nbt = new CompoundTag();
		originalTable.writeData(nbt);
		rewardTable.readData(nbt);
		callback = c;
		setTitle(new TranslatableComponent("ftbquests.reward_table"));
		setBorder(1, 1, 1);
	}

	@Override
	public void addButtons(Panel panel) {
		panel.add(new RewardTableSettingsButton(panel));
		panel.add(new SaveRewardTableButton(panel));
		panel.add(new AddWeightedRewardButton(panel));
		panel.add(new VerticalSpaceWidget(panel, 1));

		for (WeightedReward r : rewardTable.rewards) {
			panel.add(new WeightedRewardButton(panel, r));
		}
	}

	@Override
	public Theme getTheme() {
		return FTBQuestsTheme.INSTANCE;
	}
}