/**
 *  Copyright (C) 2002-2016   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.server.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel.CombatResult;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.DiplomaticTrade.TradeStatus;
import net.sf.freecol.common.model.Disaster;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Europe.MigrationType;
import net.sf.freecol.common.model.ExportData;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Force;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Market.Access;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.NativeTrade;
import net.sf.freecol.common.model.NativeTradeItem;
import net.sf.freecol.common.model.NativeTrade.NativeTradeAction;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.RandomRange;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TradeItem;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TradeRouteStop;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitChangeType;
import net.sf.freecol.common.model.UnitChangeType.UnitChange;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitLocation;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.networking.BuyPropositionMessage;
import net.sf.freecol.common.networking.ChatMessage;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.common.networking.DiplomacyMessage;
import net.sf.freecol.common.networking.ErrorMessage;
import net.sf.freecol.common.networking.GoodsForSaleMessage;
import net.sf.freecol.common.networking.InciteMessage;
import net.sf.freecol.common.networking.IndianDemandMessage;
import net.sf.freecol.common.networking.LootCargoMessage;
import net.sf.freecol.common.networking.MonarchActionMessage;
import net.sf.freecol.common.networking.NationSummaryMessage;
import net.sf.freecol.common.networking.NativeTradeMessage;
import net.sf.freecol.common.networking.RearrangeColonyMessage.Arrangement;
import net.sf.freecol.common.networking.ScoutSpeakToChiefMessage;
import net.sf.freecol.common.networking.SellPropositionMessage;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.common.util.Utils;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.REFAIPlayer;
import net.sf.freecol.server.control.ChangeSet.ChangePriority;
import net.sf.freecol.server.control.ChangeSet.See;
import net.sf.freecol.server.model.DiplomacySession;
import net.sf.freecol.server.model.LootSession;
import net.sf.freecol.server.model.MonarchSession;
import net.sf.freecol.server.model.NativeTradeSession;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerEurope;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerRegion;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.server.model.Session;


/**
 * The main server controller.
 */
public final class InGameController extends Controller {

    private static final Logger logger = Logger.getLogger(InGameController.class.getName());

    /** The server random number source. */
    private final Random random;

    /** Debug helpers, do not serialize. */
    private int debugOnlyAITurns = 0;
    private MonarchAction debugMonarchAction = null;
    private ServerPlayer debugMonarchPlayer = null;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main server object.
     * @param random The pseudo-random number source to use.
     */
    public InGameController(FreeColServer freeColServer, Random random) {
        super(freeColServer);

        this.random = random;
    }


    /**
     * Asks a question of a player with a timeout.
     *
     * @param serverPlayer The {@code ServerPlayer} to ask.
     * @param request The {@code DOMMessage} question.
     * @return The response to the question, or null if none.
     */
    private DOMMessage askTimeout(ServerPlayer serverPlayer,
                                  DOMMessage request) {
        final boolean single = getFreeColServer().getSinglePlayer();
        return getGame().askTimeout(serverPlayer, FreeCol.getTimeout(single),
                                    request);
    }


    // Debug support

    /**
     * Gets the number of AI turns to skip through.
     *
     * @return The number of terms to skip.
     */
    public int getSkippedTurns() {
        return (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS))
            ? debugOnlyAITurns : -1;
    }

    /**
     * Sets the number of AI turns to skip through as a debug helper.
     *
     * @param turns The number of turns to skip through.
     */
    public void setSkippedTurns(int turns) {
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            debugOnlyAITurns = turns;
        }
    }

    /**
     * Sets a monarch action to debug/test.
     *
     * @param serverPlayer The {@code ServerPlayer} whose monarch
     *     should act.
     * @param action The {@code MonarchAction} to be taken.
     */
    public void setMonarchAction(ServerPlayer serverPlayer,
                                 MonarchAction action) {
        if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
            debugMonarchPlayer = serverPlayer;
            debugMonarchAction = action;
        }
    }

    /**
     * Debug convenience to step the random number generator.
     *
     * @return The next random number in series, in the range 0-99.
     */
    public int stepRandom() {
        return randomInt(logger, "step random", random, 100);
    }

    /**
     * Public version of csAddFoundingFather so it can be used in the
     * test code and DebugMenu.
     *
     * @param serverPlayer The {@code ServerPlayer} who gains a father.
     * @param father The {@code FoundingFather} to add.
     */
    public void addFoundingFather(ServerPlayer serverPlayer,
                                  FoundingFather father) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csAddFoundingFather(father, random, cs);
        cs.addAttribute(See.only(serverPlayer), "flush",
                        Boolean.TRUE.toString());
        getGame().sendTo(serverPlayer, cs);
    }

    /**
     * Public change stance and inform all routine.  Mostly used in the
     * test suite, but the AIs also call it.
     *
     * @param serverPlayer The originating {@code ServerPlayer}.
     * @param stance The new {@code Stance}.
     * @param other The {@code ServerPlayer} wrt which the
     *     stance changes.
     * @param symmetric If true, change the otherPlayer stance as well.
     */
    public void changeStance(ServerPlayer serverPlayer, Stance stance,
                             ServerPlayer other, boolean symmetric) {
        ChangeSet cs = new ChangeSet();
        if (serverPlayer.csChangeStance(stance, other, symmetric, cs)) {
            getGame().sendToAll(cs);
        }
    }

    /**
     * Change colony owner.  Public for DebugUtils.
     *
     * @param colony The {@code ServerColony} to change.
     * @param serverPlayer The {@code ServerPlayer} to change to.
     */
    public void debugChangeOwner(ServerColony colony,
                                 ServerPlayer serverPlayer) {
        ChangeSet cs = new ChangeSet();
        ServerPlayer owner = (ServerPlayer)colony.getOwner();
        colony.csChangeOwner(serverPlayer, false, cs);//-vis(serverPlayer,owner)
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        owner.invalidateCanSeeTiles();//+vis(owner)
        getGame().sendToAll(cs);
    }

    /**
     * Change unit owner.  Public for DebugUtils.
     *
     * @param unit The {@code ServerUnit} to change.
     * @param serverPlayer The {@code ServerPlayer} to change to.
     */
    public void debugChangeOwner(ServerUnit unit, ServerPlayer serverPlayer) {
        ChangeSet cs = new ChangeSet();
        ServerPlayer owner = (ServerPlayer)unit.getOwner();
        owner.csChangeOwner(unit, serverPlayer, null, null,
                            cs);//-vis(serverPlayer,owner)
        cs.add(See.perhaps().always(owner), unit.getTile());
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        owner.invalidateCanSeeTiles();//+vis(owner)
        getGame().sendToAll(cs);
    }

    /**
     * Apply a disaster to a colony.  Public for DebugUtils.
     *
     * @param colony The {@code Colony} to apply the disaster to.
     * @param disaster The {@code Disaster} to apply.
     * @return The number of messages generated.
     */
    public int debugApplyDisaster(ServerColony colony, Disaster disaster) {
        ChangeSet cs = new ChangeSet();
        ServerPlayer owner = (ServerPlayer)colony.getOwner();
        List<ModelMessage> messages
            = owner.csApplyDisaster(random, colony, disaster, cs);
        if (!messages.isEmpty()) {
            cs.addMessage(See.all(),
                new ModelMessage(MessageType.DEFAULT,
                                 "model.disaster.strikes", owner)
                    .addName("%colony%", colony.getName())
                    .addName("%disaster%", disaster));
            for (ModelMessage message : messages) {
                cs.addMessage(See.all(), message);
            }
            getGame().sendToAll(cs);
        }
        return messages.size();
    }


    // Internal utilities

    /**
     * Create the Royal Expeditionary Force player corresponding to
     * a given player that is about to rebel.
     *
     * Public for the test suite.
     *
     * FIXME: this should eventually generate changes for the REF player.
     *
     * @param serverPlayer The {@code ServerPlayer} about to rebel.
     * @return The REF player.
     */
    public ServerPlayer createREFPlayer(ServerPlayer serverPlayer) {
        final Nation refNation = serverPlayer.getNation().getREFNation();
        final Monarch monarch = serverPlayer.getMonarch();
        final ServerPlayer refPlayer = getFreeColServer().makeAIPlayer(refNation);
        final Europe europe = refPlayer.getEurope();
        final Predicate<Tile> exploredPred = t ->
            ((!t.isLand() || t.isCoastland() || t.getOwner() == serverPlayer)
                && t.isExploredBy(serverPlayer));
        // Inherit rebel player knowledge of the seas, coasts, claimed
        // land but not full detailed scouting knowledge.
        Set<Tile> explore = new HashSet<>();
        getGame().getMap().forEachTile(exploredPred, t -> explore.add(t));
        refPlayer.exploreTiles(explore);

        // Trigger initial placement routine
        refPlayer.setEntryLocation(null);
        // Will change, setup only
        Player.makeContact(serverPlayer, refPlayer);

        // Instantiate the REF in Europe
        Force exf = monarch.getExpeditionaryForce();
        // @compat 0.10.5
        // There was a bug that seriously under provisioned the navy back
        // around 0.10.5, the game in BR#2435 has it.  For now, just add
        // enough ships to carry all the units.
        UnitType ut = monarch.getNavalREFUnitType();
        while (exf.getSpaceRequired() > exf.getCapacity()) {
            AbstractUnit au
                = new AbstractUnit(ut, Specification.DEFAULT_ROLE_ID, 1);
            exf.add(au);
        }
        // end @compat 0.10.5
        List<Unit> landUnits = refPlayer.createUnits(exf.getLandUnitsList(),
                                                     europe);//-vis: safe!map
        List<Unit> navalUnits = refPlayer.createUnits(exf.getNavalUnitsList(),
                                                      europe);//-vis: safe!map
        refPlayer.loadShips(landUnits, navalUnits, random);//-vis: safe!map
        return refPlayer;
    }

    /**
     * Move goods from one location to another.
     *
     * @param src The source {@code GoodsLocation}.
     * @param goodsType The {@code GoodsType} to move.
     * @param amount The amount of goods to move.
     * @param dst The new {@code GoodsLocation}.
     */
    private void moveGoods(GoodsLocation src, GoodsType goodsType, int amount,
                           GoodsLocation dst) {
        src.getGoodsContainer().saveState();
        src.removeGoods(goodsType, amount);
        if (dst != null) {
            dst.getGoodsContainer().saveState();
            dst.addGoods(goodsType, amount);
        }
    }

    /**
     * Buy goods from a native settlement.
     *
     * @param unit The {@code Unit} that is buying.
     * @param goods The {@code Goods} to buy.
     * @param price The price to pay.
     * @param sis The {@code ServerIndianSettlement} to give to.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csBuy(Unit unit, Goods goods, int price,
                       ServerIndianSettlement sis, ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)unit.getOwner();
        csVisit(owner, sis, 0, cs);
        moveGoods(sis, goods.getType(), goods.getAmount(), unit);
        cs.add(See.perhaps(), unit);
        sis.getOwner().modifyGold(price);
        owner.modifyGold(-price);
        sis.csModifyAlarm(owner, -price/50, true, cs);
        sis.updateWantedGoods();
        final Tile tile = sis.getTile();
        tile.updateIndianSettlement(owner);
        cs.add(See.only(owner), tile);
        cs.addPartial(See.only(owner), owner, "gold");
        logger.finest(owner.getSuffix() + " " + unit + " buys " + goods
                      + " at " + sis.getName() + " for " + price);
    }

    /**
     * Sell goods to a native settlement.
     *
     * @param unit The {@code Unit} that is selling.
     * @param goods The {@code Goods} to sell.
     * @param price The price to charge.
     * @param sis The {@code ServerIndianSettlement} to sell to.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csSell(Unit unit, Goods goods, int price,
                        ServerIndianSettlement sis, ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)unit.getOwner();
        csVisit(owner, sis, 0, cs);
        moveGoods(unit, goods.getType(), goods.getAmount(), sis);
        cs.add(See.perhaps(), unit);
        sis.getOwner().modifyGold(-price);
        owner.modifyGold(price);
        sis.csModifyAlarm(owner, -price/50, true, cs);
        sis.updateWantedGoods();
        final Tile tile = sis.getTile();
        tile.updateIndianSettlement(owner);
        cs.add(See.only(owner), tile);
        cs.addPartial(See.only(owner), owner, "gold");
        cs.addSale(owner, sis, goods.getType(),
                   Math.round((float)price/goods.getAmount()));
        logger.finest(owner.getSuffix() + " " + unit + " sells " + goods
                      + " at " + sis.getName() + " for " + price);
    }

    /**
     * Give goods to a native settlement.
     *
     * @param unit The {@code Unit} that is giving.
     * @param goods The {@code Goods} to give.
     * @param price A price that the natives might have been willing to pay.
     * @param sis The {@code ServerIndianSettlement} to give to.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csGift(Unit unit, Goods goods, int price,
                        ServerIndianSettlement sis, ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)unit.getOwner();
        csVisit(owner, sis, 0, cs);
        moveGoods(unit, goods.getType(), goods.getAmount(), sis);
        cs.add(See.perhaps(), unit);
        sis.csModifyAlarm(owner, -price/50, true, cs);
        sis.updateWantedGoods();
        final Tile tile = sis.getTile();
        tile.updateIndianSettlement(owner);
        cs.add(See.only(owner), tile);
        logger.finest(owner.getSuffix() + " " + unit + " gives " + goods
                      + " at " + sis.getName());
    }
    
    /**
     * Visits a native settlement, possibly scouting it full if it is
     * as a result of a scout actually asking to speak to the chief,
     * or for other settlement-contacting events such as missionary
     * actions, demanding tribute, learning skills and trading if the
     * settlementActionsContactChief game option is enabled.  It is
     * still unclear what Col1 did here.
     *
     * @param serverPlayer The {@code ServerPlayer} that is contacting
     *     the settlement.
     * @param is The {@code IndianSettlement} to contact.
     * @param scout Positive if this contact is due to a scout asking to
     *     speak to the chief, zero if it is another unit, negative if
     *     this is from the greeting dialog generation.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csVisit(ServerPlayer serverPlayer, IndianSettlement is,
                         int scout, ChangeSet cs) {
        final ServerPlayer owner = (ServerPlayer)is.getOwner();
        if (serverPlayer.csContact(owner, cs)) {
            serverPlayer.csNativeFirstContact(owner, null, cs);
        }
        is.setVisited(serverPlayer);
        if (scout > 0 || (scout == 0 && getGame().getSpecification()
                .getBoolean(GameOptions.SETTLEMENT_ACTIONS_CONTACT_CHIEF))) {
            is.setScouted(serverPlayer);
        }
    }

    /**
     * Launch the REF.
     *
     * @param serverPlayer The REF {@code ServerPlayer}.
     * @param teleport If true, teleport the REF in.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csLaunchREF(ServerPlayer serverPlayer, boolean teleport,
                             ChangeSet cs) {
        // Set the REF player entry location from the rebels.  Note
        // that the REF units will have their own unit-specific entry
        // locations set by their missions.  This just flags that the
        // REF is initialized.
        Player rebel = first(serverPlayer.getRebels());
        if (rebel != null) {
            serverPlayer.setEntryLocation(rebel.getEntryLocation().getTile());
        }

        if (teleport) { // Teleport in the units.
            Set<Tile> seen = new HashSet<>();
            for (Unit u : transform(serverPlayer.getUnits(), Unit::isNaval)) {
                Tile entry = u.getEntryLocation().getTile();
                u.setLocation(entry);//-vis(serverPlayer)
                u.setWorkLeft(-1);
                u.setState(Unit.UnitState.ACTIVE);
                if (!seen.contains(entry)) {
                    seen.add(entry);
                    cs.add(See.only(serverPlayer),
                           serverPlayer.exploreForUnit(u));
                    cs.add(See.perhaps().except(serverPlayer), entry);
                }
            }
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        } else {
            // Put navy on the high seas, with 1-turn sail time
            for (Unit u : transform(serverPlayer.getUnits(), Unit::isNaval)) {
                u.setWorkLeft(1);
                u.setDestination(u.getEntryLocation());
                u.setLocation(u.getOwner().getHighSeas());//-vis: safe!map
            }
        }
    }

    /**
     * Give independence.  Note that the REF player is granting, but
     * most of the changes happen to the newly independent player.
     * hence the special handling.
     *
     * @param serverPlayer The REF {@code ServerPlayer} that is granting.
     * @param independent The newly independent {@code ServerPlayer}.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csGiveIndependence(ServerPlayer serverPlayer,
                                    ServerPlayer independent, ChangeSet cs) {
        serverPlayer.csChangeStance(Stance.PEACE, independent, true, cs);
        independent.changePlayerType(PlayerType.INDEPENDENT);
        Game game = getGame();
        Turn turn = game.getTurn();
        independent.setTax(0);
        independent.reinitialiseMarket();
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.INDEPENDENCE, independent);

        // The score for actual independence is actually a percentage
        // bonus depending on how many other nations are independent.
        // If we ever go for a more complex scoring algorithm it might
        // be better to replace the score int with a Modifier, but for
        // now we just set the score value to the number of
        // independent players other than the one we are granting
        // here, and generate the bonus with a special case in
        // ServerPlayer.updateScore().
        int n = count(game.getLiveEuropeanPlayers(independent),
                      p -> p.getPlayerType() == PlayerType.INDEPENDENT);
        h.setScore(n);
        cs.addGlobalHistory(game, h);
        cs.addMessage(See.only(independent),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "giveIndependence.announce", independent)
                .addStringTemplate("%ref%", serverPlayer.getNationLabel()));

        // Who surrenders?
        final Predicate<Unit> surrenderPred = u -> //-vis(both)
            (u.hasTile() && !u.isNaval() && !u.isOnCarrier()
                && serverPlayer.csChangeOwner(u, independent,
                    UnitChangeType.CAPTURE, null, cs));
        List<Unit> surrenderUnits
            = transform(serverPlayer.getUnits(), surrenderPred);
        for (Unit u : surrenderUnits) {
            u.setMovesLeft(0);
            u.setState(Unit.UnitState.ACTIVE);
            cs.add(See.perhaps().always(serverPlayer), u.getTile());
        }
        if (!surrenderUnits.isEmpty()) {
            cs.addMessage(See.only(independent),
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "giveIndependence.unitsAcquired", independent)
                    .addStringTemplate("%units%",
                         unitTemplate(", ", surrenderUnits)));
            independent.invalidateCanSeeTiles();//+vis(independent)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        }

        // Update player type.  Again, a pity to have to do a whole
        // player update, but a partial update works for other players.
        cs.addPartial(See.all().except(independent), independent, "playerType");
        cs.addMessage(See.all().except(independent),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "giveIndependence.otherAnnounce", independent)
                .addStringTemplate("%nation%", independent.getNationLabel())
                .addStringTemplate("%ref%", serverPlayer.getNationLabel()));
        cs.add(See.only(independent), independent);

        // Reveal the map on independence.
        cs.add(See.only(independent), independent.exploreMap(true));
    }

    private StringTemplate unitTemplate(String base, List<Unit> units) {
        StringTemplate template = StringTemplate.label(base);
        for (Unit u : units) {
            template.addStringTemplate(u.getLabel(Unit.UnitLabelType.PLAIN));
        }
        return template;
    }

    /**
     * Resolves a tax raise.
     *
     * @param serverPlayer The {@code ServerPlayer} whose tax is rising.
     * @param taxRaise The amount of tax raise.
     * @param goods The {@code Goods} for a goods party.
     * @param result Whether the tax was accepted or not.
    private void raiseTax(ServerPlayer serverPlayer, int taxRaise, Goods goods,
                          boolean result) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csRaiseTax(taxRaise, goods, result, cs);
        getGame().sendTo(serverPlayer, cs);
    }
     */

    /**
     * Performs a monarch action.
     *
     * Note that CHANGE_LATE is used so that these actions follow
     * setting the current player, so that it is the players turn when
     * they respond to a monarch action.
     *
     * @param serverPlayer The {@code ServerPlayer} being acted upon.
     * @param action The monarch action.
     * @param cs A {@code ChangeSet} to update.
     */
    private void csMonarchAction(final ServerPlayer serverPlayer,
                                 MonarchAction action, ChangeSet cs) {
        final Monarch monarch = serverPlayer.getMonarch();
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        boolean valid = monarch.actionIsValid(action);
        if (!valid) return;
        String messageId = action.getTextKey();
        StringTemplate template;
        MonarchActionMessage message;
        String monarchKey = serverPlayer.getMonarchKey();

        switch (action) {
        case NO_ACTION:
            break;
        case RAISE_TAX_WAR: case RAISE_TAX_ACT:
            final int taxRaise = monarch.raiseTax(random);
            final Goods goods = serverPlayer.getMostValuableGoods();
            if (goods == null) {
                logger.finest("Ignoring tax raise, no goods to boycott.");
                break;
            }
            template = StringTemplate.template(messageId)
                .addStringTemplate("%goods%", goods.getType().getLabel())
                .addAmount("%amount%", taxRaise);
            if (action == MonarchAction.RAISE_TAX_WAR) {
                template = template.add("%nation%",
                    Nation.getRandomNonPlayerNationNameKey(game, random));
            } else if (action == MonarchAction.RAISE_TAX_ACT) {
                template = template.addAmount("%number%",
                    randomInt(logger, "Tax act goods", random, 6))
                    .addName("%newWorld%", serverPlayer.getNewLandName());
            }
            message = new MonarchActionMessage(action, template, monarchKey)
                .setTax(taxRaise);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_EARLY,
                   message);
            new MonarchSession(serverPlayer, action, taxRaise, goods);
            break;
        case LOWER_TAX_WAR: case LOWER_TAX_OTHER:
            int oldTax = serverPlayer.getTax();
            int taxLower = monarch.lowerTax(random);
            serverPlayer.csSetTax(taxLower, cs);
            template = StringTemplate.template(messageId)
                .addAmount("%difference%", oldTax - taxLower)
                .addAmount("%newTax%", taxLower);
            if (action == MonarchAction.LOWER_TAX_WAR) {
                template = template.add("%nation%",
                    Nation.getRandomNonPlayerNationNameKey(game, random));
            } else {
                template = template.addAmount("%number%",
                    randomInt(logger, "Lower tax reason", random, 5));
            }
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action, template, monarchKey));
            break;
        case WAIVE_TAX:
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_NORMAL,
                new MonarchActionMessage(action,
                    StringTemplate.template(messageId), monarchKey));
            break;
        case ADD_TO_REF:
            AbstractUnit refAdditions = monarch.chooseForREF(random);
            if (refAdditions == null) break;
            monarch.getExpeditionaryForce().add(refAdditions);
            template = StringTemplate.template(messageId)
                .addAmount("%number%", refAdditions.getNumber())
                .addNamed("%unit%", refAdditions.getType(spec));
            cs.add(See.only(serverPlayer), monarch);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action, template, monarchKey));
            break;
        case DECLARE_PEACE:
            List<Player> friends = monarch.collectPotentialFriends();
            if (friends.isEmpty()) break;
            Player friend = getRandomMember(logger, "Choose friend",
                                            friends, random);
            serverPlayer.csChangeStance(Stance.PEACE, (ServerPlayer)friend,
                                        true, cs);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action, StringTemplate
                    .template(messageId)
                    .addStringTemplate("%nation%", friend.getNationLabel()),
                    monarchKey));
            break;
        case DECLARE_WAR:
            List<Player> enemies = monarch.collectPotentialEnemies();
            if (enemies.isEmpty()) break;
            Player enemy = getRandomMember(logger, "Choose enemy",
                                           enemies, random);
            List<AbstractUnit> warSupport 
                = monarch.getWarSupport(enemy, random);
            int warGold = 0;
            if (!warSupport.isEmpty()) {
                serverPlayer.createUnits(warSupport,
                    serverPlayer.getEurope());//-vis: safe, Europe
                warGold = spec.getInteger(GameOptions.WAR_SUPPORT_GOLD);
                warGold += (warGold/10) * (randomInt(logger, "War support gold",
                                                     random, 5) - 2);
                serverPlayer.modifyGold(warGold);
                cs.addPartial(See.only(serverPlayer), serverPlayer,
                              "gold", "score");
                logger.fine("War support v " + enemy.getNation().getSuffix()
                    + " " + warGold + " gold + " + Messages.message(AbstractUnit
                        .getListLabel(", ", warSupport)));
            }
            serverPlayer.csChangeStance(Stance.WAR, (ServerPlayer)enemy,
                                        true, cs);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action, StringTemplate
                    .template((warSupport.isEmpty()) ? messageId
                        : "model.monarch.action.declareWarSupported.text")
                        .addStringTemplate("%nation%", enemy.getNationLabel())
                        .addStringTemplate("%force%",
                            AbstractUnit.getListLabel(", ", warSupport))
                        .addAmount("%gold%", warGold),
                    monarchKey));
            break;
        case SUPPORT_LAND: case SUPPORT_SEA:
            boolean sea = action == MonarchAction.SUPPORT_SEA;
            List<AbstractUnit> support = monarch.getSupport(random, sea);
            if (support.isEmpty()) break;
            serverPlayer.createUnits(support,
                serverPlayer.getEurope());//-vis: safe, Europe
            cs.add(See.only(serverPlayer), serverPlayer.getEurope());
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new MonarchActionMessage(action, StringTemplate
                    .template(messageId)
                    .addStringTemplate("%addition%",
                        AbstractUnit.getListLabel(", ", support)),
                    monarchKey));
            break;
        case MONARCH_MERCENARIES:
            final List<AbstractUnit> mercenaries
                = monarch.getMercenaries(random);
            if (mercenaries.isEmpty()) break;
            final int mercPrice = serverPlayer.priceMercenaries(mercenaries);
            message = new MonarchActionMessage(action, StringTemplate
                .template(messageId)
                .addAmount("%gold%", mercPrice)
                .addStringTemplate("%mercenaries%",
                    AbstractUnit.getListLabel(", ", mercenaries)),
                monarchKey);
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_EARLY,
                   message);
            new MonarchSession(serverPlayer, action, mercenaries, mercPrice);
            break;
        case HESSIAN_MERCENARIES:
            serverPlayer.csMercenaries(monarch.getMercenaries(random), action,
                                       random, cs);
            break;
        case DISPLEASURE: default:
            logger.warning("Bogus action: " + action);
            break;
        }
    }


    // Diplomacy support

    /**
     * Accept a diplomatic trade.  Handles the transfers of TradeItems.
     *
     * Note that first contact contexts may not necessarily have a settlement,
     * but this is ok because first contact trade can only include stance
     * and gold trade items.
     *
     * @param agreement The {@code DiplomacyTrade} agreement.
     * @param session The {@code DiplomacySession} in scope.
     * @param cs A {@code ChangeSet} to update.
     * @return True if the trade was valid.
     */
    private boolean csAcceptTrade(DiplomaticTrade agreement,
                                  DiplomacySession session, ChangeSet cs) {
        final ServerPlayer srcPlayer = (ServerPlayer)agreement.getSender();
        final ServerPlayer dstPlayer = (ServerPlayer)agreement.getRecipient();
        final Unit unit = session.getUnit();
        final Settlement settlement = session.getSettlement();
        boolean visibilityChange = false;

        // Check trade carefully before committing.
        boolean fail = false;
        for (TradeItem tradeItem : agreement.getTradeItems()) {
            final ServerPlayer source = (ServerPlayer)tradeItem.getSource();
            final ServerPlayer dest = (ServerPlayer)tradeItem.getDestination();
            if (!tradeItem.isValid()) {
                logger.warning("Trade with invalid tradeItem: " + tradeItem);
                fail = true;
                continue;
            }
            if (source != srcPlayer && source != dstPlayer) {
                logger.warning("Trade with invalid source: "
                               + ((source == null) ? "null" : source.getId()));
                fail = true;
                continue;
            }
            if (dest != srcPlayer && dest != dstPlayer) {
                logger.warning("Trade with invalid destination: "
                               + ((dest == null) ? "null" : dest.getId()));
                fail = true;
                continue;
            }

            Colony colony = tradeItem.getColony(getGame());
            if (colony != null && !source.owns(colony)) {
                logger.warning("Trade with invalid source owner: " + colony);
                fail = true;
                continue;
            }
            int gold = tradeItem.getGold();
            if (gold > 0 && !source.checkGold(gold)) {
                logger.warning("Trade with invalid gold: " + gold);
                fail = true;
                continue;
            }

            Goods goods = tradeItem.getGoods();
            if (goods != null) {
                Location loc = goods.getLocation();
                if (loc instanceof Ownable
                    && !source.owns((Ownable)loc)) {
                    logger.warning("Trade with invalid source owner: " + loc);
                    fail = true;
                } else if (!(loc instanceof GoodsLocation
                        && loc.contains(goods))) {
                    logger.warning("Trade of unavailable goods " + goods
                        + " at " + loc);
                    fail = true;
                } else if (dest.owns(unit) && !unit.couldCarry(goods)) {
                    logger.warning("Trade unit can not carry traded goods: "
                        + goods);
                    fail = true;
                }
            }

            // Stance trade fail is harmless
            Unit u = tradeItem.getUnit();
            if (u != null) {
                if (!source.owns(u)) {
                    logger.warning("Trade with invalid source owner: " + u);
                    fail = true;
                    continue;
                } else if (dest.owns(unit) && !unit.couldCarry(u)) {
                    logger.warning("Trade unit can not carry traded unit: "
                        + u);
                    fail = true;
                }
            }
        }
        if (fail) return false;

        for (TradeItem tradeItem : agreement.getTradeItems()) {
            final ServerPlayer source = (ServerPlayer)tradeItem.getSource();
            final ServerPlayer dest = (ServerPlayer)tradeItem.getDestination();
            // Collect changes for updating.  Not very OO but
            // TradeItem should not know about server internals.
            // Take care to show items that change hands to the *old*
            // owner too.
            Stance stance = tradeItem.getStance();
            if (stance != null
                && !source.csChangeStance(stance, dest, true, cs)) {
                logger.warning("Stance trade failure: " + stance);
            }
            Colony colony = tradeItem.getColony(getGame());
            if (colony != null) {
                ((ServerColony)colony).csChangeOwner(dest, false, cs);//-vis(both),-til
                visibilityChange = true;
            }
            int gold = tradeItem.getGold();
            if (gold > 0) {
                source.modifyGold(-gold);
                dest.modifyGold(gold);
                cs.addPartial(See.only(source), source, "gold", "score");
                cs.addPartial(See.only(dest), dest, "gold", "score");
            }
            Goods goods = tradeItem.getGoods();
            if (goods != null && settlement != null) {
                if (dest.owns(settlement)) {
                    goods.setLocation(unit);
                    moveGoods(unit, goods.getType(), goods.getAmount(), settlement);
                    cs.add(See.only(source), unit);
                    cs.add(See.only(dest), settlement.getGoodsContainer());
                } else {
                    goods.setLocation(settlement);
                    moveGoods(settlement, goods.getType(), goods.getAmount(), unit);
                    cs.add(See.only(dest), unit);
                    cs.add(See.only(source), settlement.getGoodsContainer());
                }
            }
            ServerPlayer victim = (ServerPlayer)tradeItem.getVictim();
            if (victim != null) {
                if (source.csChangeStance(Stance.WAR, victim, true, cs)) {
                    // Have to add in an explicit stance change and
                    // message because the player does not normally
                    // have visibility of stance changes between other nations.
                    cs.addStance(See.only(dest), source, Stance.WAR, victim);
                    cs.addMessage(See.only(dest),
                        new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                         Stance.WAR.getOtherStanceChangeKey(),
                                         source)
                            .addStringTemplate("%attacker%",
                                source.getNationLabel())
                            .addStringTemplate("%defender%",
                                victim.getNationLabel()));
                } else {
                    logger.warning("Incite trade failure: " + victim);
                }
            }                
            ServerUnit newUnit = (ServerUnit)tradeItem.getUnit();
            if (newUnit != null && settlement != null) {
                ServerPlayer former = (ServerPlayer)newUnit.getOwner();
                Tile oldTile = newUnit.getTile();
                Location newLoc;
                if (unit.isOnCarrier()) {
                    Unit carrier = unit.getCarrier();
                    if (!carrier.couldCarry(newUnit)) {
                        logger.warning("Can not add " + newUnit
                            + " to " + carrier);
                        continue;
                    }
                    newLoc = carrier;
                } else if (dest == unit.getOwner()) {
                    newLoc = unit.getTile();
                } else {
                    newLoc = settlement.getTile();
                }
                if (source.csChangeOwner(newUnit, dest, UnitChangeType.CAPTURE,
                                         newLoc, cs)) {//-vis(both)
                    newUnit.setMovesLeft(0);
                    cs.add(See.perhaps().always(former), oldTile);
                }
                visibilityChange = true;
            }
        }
        if (visibilityChange) {
            srcPlayer.invalidateCanSeeTiles();//+vis(srcPlayer)
            dstPlayer.invalidateCanSeeTiles();//+vis(dstPlayer)
        }
        return true;
    }

    /**
     * Process a European diplomacy session according to an agreement.
     *
     * @param serverPlayer The {@code ServerPlayer} in the session.
     * @param otherPlayer The other {@code ServerPlayer} in the session.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @param session The {@code DiplomacySession} underway.
     * @param message A {@code DiplomacyMessage} to send.
     * @param cs A {@code ChangeSet} to contain the trade changes
     *     if accepted.
     * @return True if a new DiplomacyMessage reply needs to be sent.
     */
    private boolean csDiplomacySession(ServerPlayer serverPlayer,
                                       ServerPlayer otherPlayer,
                                       DiplomaticTrade agreement, 
                                       DiplomacySession session,
                                       DiplomacyMessage message,
                                       ChangeSet cs) {
        // Consider the status, process acceptance and rejection,
        // which need no further action.
        TradeStatus status = agreement.getStatus();
        switch (status) {
        case PROPOSE_TRADE:
            session.setAgreement(agreement);
            break;
        case ACCEPT_TRADE:
            // Process the agreement that was sent!
            agreement = session.getAgreement();
            agreement.setStatus(TradeStatus.ACCEPT_TRADE);
            if (csAcceptTrade(agreement, session, cs)) {
                session.complete(cs);
                getGame().sendToOthers(serverPlayer, cs);
                return false;
            }
            // Trade was invalid, fall through into rejection.
        case REJECT_TRADE: default:
            agreement = session.getAgreement();
            agreement.setStatus(TradeStatus.REJECT_TRADE);
            session.complete(cs);
            getGame().sendToOthers(serverPlayer, cs);
            return false;
        }

        // Ask the other player to consider the agreement.
        // Treat a missing reply as a rejection.
        DOMMessage reply = askTimeout(otherPlayer, message);
        if (reply instanceof DiplomacyMessage) {
            agreement = ((DiplomacyMessage)reply).getAgreement();
            status = agreement.getStatus();
        } else {
            status = TradeStatus.REJECT_TRADE;
            agreement.setStatus(status);
        }
        agreement.incrementVersion();

        // Process the result.  Always return true here as the serverPlayer
        // needs to be informed of the other player's response.
        switch (status) {
        case PROPOSE_TRADE:
            session.setAgreement(agreement);
            break;
        case ACCEPT_TRADE:
            agreement = session.getAgreement(); // Accept offered agreement
            agreement.setStatus(TradeStatus.ACCEPT_TRADE);
            if (csAcceptTrade(agreement, session, cs)) {
                session.complete(cs);
                getGame().sendToOthers(serverPlayer, cs);
                break;
            }
            // Trade was invalid, fall through into rejection.
        case REJECT_TRADE: default:
            agreement.setStatus(TradeStatus.REJECT_TRADE);
            session.setAgreement(agreement);
            session.complete(cs);
            getGame().sendToOthers(serverPlayer, cs);
            break;
        }
        return true;
    }


    // Routines that follow implement the controller response to
    // messages.  The convention is to return a change set back to the
    // invoking message handler, but handle changes for other players
    // directly here.


    /**
     * Abandon a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is abandoning.
     * @param settlement The {@code Settlement} to abandon.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet abandonSettlement(ServerPlayer serverPlayer,
                                       Settlement settlement) {
        ChangeSet cs = new ChangeSet();

        // Drop trade routes and create history event before disposing.
        if (settlement instanceof Colony) {
            serverPlayer.csLoseLocation(settlement, cs);
            cs.addHistory(serverPlayer,
                new HistoryEvent(getGame().getTurn(),
                    HistoryEvent.HistoryEventType.ABANDON_COLONY, serverPlayer)
                    .addName("%colony%", settlement.getName()));
        }

        // Comprehensive dispose.
        serverPlayer.csDisposeSettlement(settlement, cs);//+vis

        // FIXME: Player.settlements is still being fixed on the client side.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Ask about learning a skill at a native settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is learning.
     * @param unit The {@code Unit} that is learning.
     * @param is The {@code IndianSettlement} to learn from.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet askLearnSkill(ServerPlayer serverPlayer, Unit unit,
                                   IndianSettlement is) {
        ChangeSet cs = new ChangeSet();

        csVisit(serverPlayer, is, 0, cs);
        Tile tile = is.getTile();
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");

        // Do not update others, nothing to see yet.
        return cs;
    }


    /**
     * Assign a student to a teacher.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param student The student {@code Unit}.
     * @param teacher The teacher {@code Unit}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet assignTeacher(ServerPlayer serverPlayer, Unit student,
                                   Unit teacher) {
        Unit oldStudent = teacher.getStudent();
        Unit oldTeacher = student.getTeacher();

        // Only update units that changed their teaching situation.
        ChangeSet cs = new ChangeSet();
        if (oldTeacher != null) {
            oldTeacher.setStudent(null);
            cs.add(See.only(serverPlayer), oldTeacher);
        }
        if (oldStudent != null) {
            oldStudent.setTeacher(null);
            cs.add(See.only(serverPlayer), oldStudent);
        }
        teacher.setStudent(student);
        teacher.changeWorkType(null);
        student.setTeacher(teacher);
        cs.add(See.only(serverPlayer), student, teacher);
        return cs;
    }


    /**
     * Assign a trade route to a unit.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The unit {@code Unit} to assign to.
     * @param tradeRoute The {@code TradeRoute} to assign.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet assignTradeRoute(ServerPlayer serverPlayer, Unit unit,
                                      TradeRoute tradeRoute) {
        // If clearing a trade route and the unit is at sea, set
        // the destination to the next stop.  Otherwise just clear
        // the destination.
        TradeRouteStop stop;
        unit.setDestination((tradeRoute == null && unit.isAtSea()
                && (stop = unit.getStop()) != null) ? stop.getLocation()
            : null);
        unit.setTradeRoute(tradeRoute);
        if (tradeRoute != null) {
            List<TradeRouteStop> stops = tradeRoute.getStops();
            int found = -1;
            for (int i = 0; i < stops.size(); i++) {
                if (Map.isSameLocation(unit.getLocation(),
                                       stops.get(i).getLocation())) {
                    found = i;
                    break;
                }
            }
            if (found < 0) found = 0;
            unit.setCurrentStop(found);
        }

        // Only visible to the player
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Build a settlement.
     *
     * +til: Resolves many tile appearance changes.
     *
     * @param serverPlayer The {@code ServerPlayer} that is building.
     * @param unit The {@code Unit} that is building.
     * @param name The new settlement name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet buildSettlement(ServerPlayer serverPlayer, Unit unit,
                                     String name) {
        final ServerGame game = getGame();
        final Specification spec = game.getSpecification();
        ChangeSet cs = new ChangeSet();

        // Build settlement
        Tile tile = unit.getTile();
        Settlement settlement;
        if (Player.ASSIGN_SETTLEMENT_NAME.equals(name)) {
            name = serverPlayer.getSettlementName(random);
        }
        if (serverPlayer.isEuropean()) {
            StringTemplate nation = serverPlayer.getNationLabel();
            settlement = new ServerColony(game, serverPlayer, name, tile);
            for (Tile t : tile.getSurroundingTiles(settlement.getRadius())) {
                t.cacheUnseen();//+til
            }
            serverPlayer.addSettlement(settlement);
            settlement.placeSettlement(false);//-vis(serverPlayer,?),-til
            cs.add(See.only(serverPlayer),
                   serverPlayer.exploreForSettlement(settlement));

            cs.addHistory(serverPlayer, new HistoryEvent(game.getTurn(),
                    HistoryEvent.HistoryEventType.FOUND_COLONY, serverPlayer)
                .addName("%colony%", settlement.getName()));

            // Remove equipment from founder in case role confuses
            // placement.
            settlement.equipForRole(unit, spec.getDefaultRole(), 0);

            // Coronado
            for (ServerPlayer sp : transform(game.getConnectedPlayers(serverPlayer),
                    p -> p.hasAbility(Ability.SEE_ALL_COLONIES))) {
                cs.add(See.only(sp), sp.exploreForSettlement(settlement));//-vis(sp)
                sp.invalidateCanSeeTiles();//+vis(sp)
                cs.addMessage(See.only(sp),
                    new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                     "buildColony.others", settlement)
                        .addStringTemplate("%nation%", nation)
                        .addStringTemplate("%colony%",
                            settlement.getLocationLabelFor(sp))
                        .addStringTemplate("%region%",
                            tile.getRegion().getLabel()));
            }
        } else {
            IndianNationType nationType
                = (IndianNationType) serverPlayer.getNationType();
            UnitType skill = RandomChoice
                .getWeightedRandom(logger, "Choose skill",
                                   nationType.generateSkillsForTile(tile),
                                   random);
            if (skill == null) { // Seasoned Scout
                List<UnitType> scouts = spec
                    .getUnitTypesWithAbility(Ability.EXPERT_SCOUT);
                skill = getRandomMember(logger, "Choose scout", scouts, random);
            }
            settlement = new ServerIndianSettlement(game, serverPlayer, name,
                                                    tile, false, skill, null);
            for (Tile t : tile.getSurroundingTiles(settlement.getRadius())) {
                t.cacheUnseen();//+til
            }
            serverPlayer.addSettlement(settlement);
            settlement.placeSettlement(true);//-vis(serverPlayer),-til
            for (Player p : getGame().getLivePlayerList(serverPlayer)) {
                ((IndianSettlement)settlement).setAlarm(p, (p.isIndian())
                    ? new Tension(Tension.Level.CONTENT.getLimit())
                    : serverPlayer.getTension(p));//-til
            }
        }

        // Join the settlement.
        unit.setLocation(settlement);//-vis(serverPlayer),-til
        unit.setMovesLeft(0);

        // Update with settlement tile, and newly owned tiles.
        cs.add(See.perhaps(), settlement.getOwnedTiles());
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Others can see tile changes.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Buy from a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is buying.
     * @param unit The {@code Unit} that will carry the goods.
     * @param is The {@code IndianSettlement} to buy from.
     * @param goods The {@code Goods} to buy.
     * @param amount How much gold to pay.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet buyFromSettlement(ServerPlayer serverPlayer, Unit unit,
                                       IndianSettlement is,
                                       Goods goods, int amount) {
        ChangeSet cs = new ChangeSet();
        csVisit(serverPlayer, is, 0, cs);

        NativeTradeSession session
            = Session.lookup(NativeTradeSession.class, unit, is);
        if (session == null) {
            return serverPlayer.clientError("Trying to buy without opening a session");
        }
        NativeTrade nt = session.getNativeTrade();
        if (!nt.getBuy()) {
            return serverPlayer.clientError("Trying to buy in a session where buying is not allowed.");
        }
        if (!unit.hasSpaceLeft()) {
            return serverPlayer.clientError("Unit is full, unable to buy.");
        }

        // Check that this is the agreement that was made
        AIPlayer ai = getFreeColServer().getAIPlayer(is.getOwner());
        int returnGold = ai.buyProposition(unit, is, goods, amount);
        if (returnGold != amount) {
            return serverPlayer.clientError("This was not the price we agreed upon! Cheater?");
        }
        if (!serverPlayer.checkGold(amount)) { // Check this is funded.
            return serverPlayer.clientError("Insufficient gold to buy.");
        }

        // Valid, make the trade.
        moveGoods(is, goods.getType(), goods.getAmount(), unit);
        cs.add(See.perhaps(), unit);

        Player settlementPlayer = is.getOwner();
        Tile tile = is.getTile();
        is.updateWantedGoods();
        settlementPlayer.modifyGold(amount);
        serverPlayer.modifyGold(-amount);
        ((ServerIndianSettlement)is).csModifyAlarm(serverPlayer,
            -amount / 50, true, cs);
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        nt.setBuy(true);
        logger.finest(serverPlayer.getName() + " " + unit + " buys " + goods
                      + " at " + is.getName() + " for " + amount);

        // Others can see the unit capacity.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Buy goods in Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} that is buying.
     * @param type The {@code GoodsType} to buy.
     * @param amount The amount of goods to buy.
     * @param carrier The {@code Unit} to carry the goods.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet buyGoods(ServerPlayer serverPlayer, GoodsType type,
                              int amount, Unit carrier) {
        if (!serverPlayer.canTrade(type, Access.EUROPE)) {
            return serverPlayer.clientError("Can not trade boycotted goods");
        }

        ChangeSet cs = new ChangeSet();
        GoodsContainer container = carrier.getGoodsContainer();
        container.saveState();
        int gold = serverPlayer.getGold();
        int buyAmount = serverPlayer.buy(container, type, amount);
        if (buyAmount < 0) {
            return serverPlayer.clientError("Player " + serverPlayer.getName()
                + " tried to buy " + amount + " " + type.getSuffix());
        }
        serverPlayer.propagateToEuropeanMarkets(type, -buyAmount, random);
        serverPlayer.csFlushMarket(type, cs);
        carrier.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), carrier);
        logger.finest(carrier + " bought " + amount + "(" + buyAmount + ")"
            + " " + type.getSuffix()
            + " in Europe for " + (serverPlayer.getGold() - gold));
        // Action occurs in Europe, nothing is visible to other players.
        return cs;
    }


    /**
     * Price some goods for sale from a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is buying.
     * @param unit The {@code Unit} that is trading.
     * @param settlement The {@code Settlement} that is trading.
     * @param goods The {@code Goods} to buy.
     * @param price The buyers proposed price for the goods.
     * @return A {@code BuyPropositionMessage} encapsulating this action,
     *     or an error message on failure.
     */
    public DOMMessage buyProposition(ServerPlayer serverPlayer,
        Unit unit, Settlement settlement, Goods goods, int price) {
        NativeTradeSession session
            = Session.lookup(NativeTradeSession.class, unit, settlement);
        if (session == null) {
            return new ErrorMessage("Proposing to buy without opening a session?!");
        }
        NativeTrade nt = session.getNativeTrade();
        if (!nt.getBuy()) {
            return new ErrorMessage("Proposing to buy in a session where buying is not allowed.");
        }
        ChangeSet cs = new ChangeSet();
        if (settlement instanceof IndianSettlement) {
            csVisit(serverPlayer, (IndianSettlement)settlement, 0, cs);
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = getFreeColServer().getAIPlayer(settlement.getOwner());
        int gold = ai.buyProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        return new BuyPropositionMessage(unit, settlement, goods, gold);
    }


    /**
     * Cash in a treasure train.
     *
     * @param serverPlayer The {@code ServerPlayer} that is cashing in.
     * @param unit The treasure train {@code Unit} to cash in.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet cashInTreasureTrain(ServerPlayer serverPlayer, Unit unit) {
        ChangeSet cs = new ChangeSet();

        // Work out the cash in amount and the message to send.
        int fullAmount = unit.getTreasureAmount();
        int cashInAmount;
        String messageId;
        if (serverPlayer.getPlayerType() == PlayerType.COLONIAL) {
            // Charge transport fee and apply tax
            cashInAmount = (fullAmount - unit.getTransportFee())
                * (100 - serverPlayer.getTax()) / 100;
            messageId = "cashInTreasureTrain.colonial";
        } else {
            // No fee possible, no tax applies.
            cashInAmount = fullAmount;
            messageId = "cashInTreasureTrain.independent";
        }

        serverPlayer.modifyGold(cashInAmount);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold", "score");
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             messageId, serverPlayer, unit)
                .addAmount("%amount%", fullAmount)
                .addAmount("%cashInAmount%", cashInAmount));
        messageId = (serverPlayer.isRebel()
                     || serverPlayer.getPlayerType() == PlayerType.INDEPENDENT)
            ? "cashInTreasureTrain.otherIndependent"
            : "cashInTreasureTrain.otherColonial";
        cs.addMessage(See.all().except(serverPlayer),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             messageId, serverPlayer)
                .addAmount("%amount%", fullAmount)
                .addStringTemplate("%nation%", serverPlayer.getNationLabel()));

        // Dispose of the unit, only visible to the owner.
        cs.add(See.only(serverPlayer), (FreeColGameObject)unit.getLocation());
        cs.addRemove(See.only(serverPlayer), null, unit);//-vis: safe in colony
        unit.dispose();

        // Others can see the cash in message.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Change a units state.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the state of.
     * @param state The new {@code UnitState}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet changeState(ServerPlayer serverPlayer, Unit unit,
                                 UnitState state) {
        ChangeSet cs = new ChangeSet();

        Tile tile = unit.getTile();
        boolean tileDirty = tile != null && tile.getIndianSettlement() != null;
        if (state == UnitState.FORTIFYING && tile != null) {
            ServerColony colony = (tile.getOwningSettlement() instanceof Colony)
                ? (ServerColony) tile.getOwningSettlement()
                : null;
            Player owner = (colony == null) ? null : colony.getOwner();
            if (owner != null
                && owner != unit.getOwner()
                && serverPlayer.getStance(owner) != Stance.ALLIANCE
                && serverPlayer.getStance(owner) != Stance.PEACE) {
                if (colony.isTileInUse(tile)) {
                    colony.csEvictUsers(unit, cs);
                }
                if (serverPlayer.getStance(owner) == Stance.WAR) {
                    tile.changeOwnership(null, null); // Clear owner if at war
                    tileDirty = true;
                }
            }
        }

        unit.setState(state);
        if (tileDirty) {
            cs.add(See.perhaps(), tile);
        } else {
            cs.add(See.perhaps(), (FreeColGameObject)unit.getLocation());
        }

        // Others might be able to see the unit.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Change improvement work type.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the work type of.
     * @param type The new {@code TileImprovementType} to produce.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet changeWorkImprovementType(ServerPlayer serverPlayer,
                                               Unit unit,
                                               TileImprovementType type) {
        Tile tile = unit.getTile();
        TileImprovement improvement = tile.getTileImprovement(type);
        if (improvement == null) { // Create the new improvement.
            improvement = new TileImprovement(getGame(), tile, type, null);
            tile.add(improvement);
        }

        unit.setWorkImprovement(improvement);
        unit.setState(UnitState.IMPROVING);

        // Private update of the tile.
        return new ChangeSet().add(See.only(serverPlayer), tile);
    }


    /**
     * Change work type.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the work type of.
     * @param type The new {@code GoodsType} to produce.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet changeWorkType(ServerPlayer serverPlayer, Unit unit,
                                    GoodsType type) {
        if (unit.getWorkType() != type) {
            unit.setExperience(0);
            unit.changeWorkType(type);
        }

        // Private update of the colony.
        return new ChangeSet().add(See.only(serverPlayer), unit.getColony());
    }


    /**
     * Chat.
     *
     * @param serverPlayer The {@code ServerPlayer} that is chatting.
     * @param message The chat message.
     * @param pri A privacy setting, currently a noop.
     */
    public void chat(ServerPlayer serverPlayer, String message, boolean pri) {
        getGame().sendToOthers(serverPlayer, new ChangeSet()
            .add(See.all().except(serverPlayer),
                 ChangeSet.ChangePriority.CHANGE_NORMAL,
                 new ChatMessage(serverPlayer, message, false)));
    }


    /**
     * Claim land.
     *
     * @param serverPlayer The {@code ServerPlayer} claiming.
     * @param tile The {@code Tile} to claim.
     * @param settlement The {@code Settlement} to claim for.
     * @param price The price to pay for the land, which must agree
     *     with the owner valuation, unless negative which denotes stealing.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet claimLand(ServerPlayer serverPlayer, Tile tile,
                               Settlement settlement, int price) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csClaimLand(tile, settlement, price, cs);

        if (settlement != null && serverPlayer.isEuropean()) {
            // Define Coronado to make all colony-owned tiles visible
            for (ServerPlayer sp
                     : transform(getGame().getConnectedPlayers(serverPlayer),
                         p -> p.hasAbility(Ability.SEE_ALL_COLONIES))) {
                sp.exploreTile(tile);
                cs.add(See.only(sp), tile);
                sp.invalidateCanSeeTiles();//+vis(sp)
            }
        }

        // Others can see the tile.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Clear the specialty of a unit.
     *
     * FIXME: why not clear speciality in the open?  You can disband!
     * If we implement this remember to fix the visibility.
     *
     * @param serverPlayer The owner of the unit.
     * @param unit The {@code Unit} to clear the speciality of.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet clearSpeciality(ServerPlayer serverPlayer, Unit unit) {
        UnitChange uc = unit.getUnitChange(UnitChangeType.CLEAR_SKILL);
        if (uc == null) {
            return serverPlayer.clientError("Can not clear unit speciality: "
                + unit.getId());
        }

        // There can be some restrictions that may prevent the
        // clearing of the speciality.  AFAICT the only ATM is that a
        // teacher can not lose its speciality, but this will need to
        // be revisited if we invent a work location that requires a
        // particular unit type.
        if (unit.getStudent() != null) {
            return serverPlayer.clientError("Can not clear speciality of a teacher.");
        }

        // Valid, change type.
        unit.changeType(uc.to);//-vis: safe in colony

        // Update just the unit, others can not see it as this only happens
        // in-colony.
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Combat.  Public for the test suite.
     *
     * @param attackerPlayer The {@code ServerPlayer} who is attacking.
     * @param attacker The {@code FreeColGameObject} that is attacking.
     * @param defender The {@code FreeColGameObject} that is defending.
     * @param crs A list of {@code CombatResult}s defining the result.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet combat(ServerPlayer attackerPlayer,
                            FreeColGameObject attacker,
                            FreeColGameObject defender,
                            List<CombatResult> crs) {
        ChangeSet cs = new ChangeSet();
        try {
            attackerPlayer.csCombat(attacker, defender, crs, random, cs);
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Combat FAIL", e);
            return attackerPlayer.clientError(e.getMessage());
        }
        getGame().sendToOthers(attackerPlayer, cs);
        return cs;
    }


    /**
     * Continue playing after winning.
     *
     * @param serverPlayer The {@code ServerPlayer} that plays on.
     */
    public void continuePlaying(ServerPlayer serverPlayer) {
        final ServerGame game = getGame();
        if (!getFreeColServer().getSinglePlayer()) {
            logger.warning("Can not continue playing in multiplayer!");
        } else if (serverPlayer != game.checkForWinner()) {
            logger.warning("Can not continue playing, as "
                           + serverPlayer.getName()
                           + " has not won the game!");
        } else {
            final Specification spec = game.getSpecification();
            spec.getBooleanOption(GameOptions.VICTORY_DEFEAT_REF)
                .setValue(Boolean.FALSE);
            spec.getBooleanOption(GameOptions.VICTORY_DEFEAT_EUROPEANS)
                .setValue(Boolean.FALSE);
            spec.getBooleanOption(GameOptions.VICTORY_DEFEAT_HUMANS)
                .setValue(Boolean.FALSE);
            logger.info("Disabled victory conditions, as "
                        + serverPlayer.getName()
                        + " has won, but is continuing to play.");
        }
    }


    /**
     * Declare independence.
     *
     * @param serverPlayer The {@code ServerPlayer} that is declaring.
     * @param nationName The new name for the independent nation.
     * @param countryName The new name for its residents.
     * @return A {@code ChangeSet} containing the response.
     */
    public ChangeSet declareIndependence(final ServerPlayer serverPlayer,
                                         String nationName, String countryName) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        ChangeSet cs = new ChangeSet();

        // Cross the Rubicon
        StringTemplate oldNation = serverPlayer.getNationLabel();
        serverPlayer.setIndependentNationName(nationName);
        serverPlayer.setNewLandName(countryName);
        serverPlayer.changePlayerType(PlayerType.REBEL);

        // Do not add history event to cs as we are going to update the
        // entire player.  Likewise clear model messages.
        Turn turn = game.getTurn();
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.DECLARE_INDEPENDENCE, serverPlayer);
        final int independenceTurn = spec.getInteger(GameOptions.INDEPENDENCE_TURN);
        h.setScore(Math.max(0, independenceTurn - turn.getNumber()));
        cs.addGlobalHistory(game, h);
        serverPlayer.clearModelMessages();
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "declareIndependence.resolution", serverPlayer));

        // Dispose of units in or heading to Europe.
        Europe europe = serverPlayer.getEurope();
        StringTemplate seized = StringTemplate.label(", ");
        boolean lost = false;
        for (Unit u : europe.getUnitList()) {
            seized.addStringTemplate(u.getLabel());
            cs.addRemove(See.only(serverPlayer), null, u);
            u.dispose();
            lost = true;
        }
        for (Unit u : transform(serverPlayer.getHighSeas().getUnits(),
                                matchKey(europe, Unit::getDestination))) {
            seized.addStringTemplate(u.getLabel());
            cs.addRemove(See.only(serverPlayer), null, u);
            u.dispose();
            lost = true;
        }
        if (lost) {
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(MessageType.UNIT_LOST,
                                 "declareIndependence.unitsSeized",
                                 serverPlayer)
                    .addStringTemplate("%units%", seized));
        }
        serverPlayer.csLoseLocation(europe, cs);

        // Create the REF.
        ServerPlayer refPlayer = createREFPlayer(serverPlayer);
        cs.addPlayer(refPlayer);
        // Update the intervention force
        final Monarch monarch = serverPlayer.getMonarch();
        monarch.updateInterventionForce();
        String otherKey = Nation.getRandomNonPlayerNationNameKey(game, random);
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "declareIndependence.interventionForce",
                             serverPlayer)
                .add("%nation%", otherKey)
                .addAmount("%number%",
                    spec.getInteger(GameOptions.INTERVENTION_BELLS)));
        serverPlayer.csChangeStance(Stance.WAR, refPlayer, true, cs);

        // Generalized continental army muster.
        // Do not use UnitType.getTargetType.
        java.util.Map<UnitType, List<Unit>> unitMap = new HashMap<>();
        for (Colony colony : transform(serverPlayer.getColonies(),
                                       c -> c.getSoL() > 50)) {
            List<Unit> allUnits = colony.getAllUnitsList();
            int limit = (allUnits.size() + 2) * (colony.getSoL() - 50) / 100;

            unitMap.clear();
            for (Unit unit : transform(allUnits,
                    u -> u.getUnitChange(UnitChangeType.INDEPENDENCE) != null)) {
                appendToMapList(unitMap, unit.getType(), unit);
            }
            for (Entry<UnitType, List<Unit>> entry : unitMap.entrySet()) {
                int n = 0;
                UnitType fromType = entry.getKey();
                UnitType toType = spec.getUnitChange(UnitChangeType.INDEPENDENCE,
                                                     fromType).to;
                List<Unit> units = entry.getValue();
                while (!units.isEmpty() && n < limit) {
                    Unit unit = units.remove(0);
                    unit.changeType(toType);//-vis
                    cs.add(See.only(serverPlayer), unit);
                    n++;
                }
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(MessageType.UNIT_IMPROVED,
                                     "declareIndependence.continentalArmyMuster",
                                     serverPlayer, colony)
                        .addName("%colony%", colony.getName())
                        .addAmount("%number%", n)
                        .addNamed("%oldUnit%", fromType)
                        .addNamed("%unit%", toType));
                limit -= n;
            }
        }

        // The most hostile contacted non-warring natives declare war
        // on you and peace with the REF, least hostile contacted
        // natives declare peace on you and war on the REF.  If they
        // are the same nation, go to the next most hostile nation
        // that may already be at war.
        final Comparator<Player> comp = Comparator.comparingInt(p ->
            p.getTension(serverPlayer).getValue());
        List<Player> natives = transform(game.getLiveNativePlayers(),
            p -> p.hasContacted(serverPlayer), Function.identity(), comp);
        if (!natives.isEmpty()) {
            ServerPlayer good = (ServerPlayer)first(natives);
            logger.info("Native ally following independence: " + good);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "declareIndependence.nativeSupport", good)
                    .addStringTemplate("%nation%", good.getNationLabel())
                    .add("%ruler%", serverPlayer.getRulerNameKey()));
            int delta;
            switch (good.getStance(serverPlayer)) {
            case ALLIANCE: case PEACE: default:
                delta = 0;
                break;
            case CEASE_FIRE:
                delta = Tension.Level.HAPPY.getLimit()
                    - good.getTension(serverPlayer).getValue();
                break;
            case WAR:
                delta = Tension.Level.CONTENT.getLimit()
                    - good.getTension(serverPlayer).getValue();
                break;
            }
            good.csModifyTension(serverPlayer, delta, cs);
            Player.makeContact(good, refPlayer);
            good.csModifyTension(refPlayer,
                Tension.Level.HATEFUL.getLimit(), cs);

            reverse(natives);
            ServerPlayer bad = null;
            for (Player p : natives) {
                if (p == good
                    || p.getStance(serverPlayer) == Stance.ALLIANCE) break;
                bad = (ServerPlayer)p;
                if (!p.atWarWith(serverPlayer)) break;
            }
            logger.info("Native enemy following independence: " + bad);
            if (bad != null) {
                switch (bad.getStance(serverPlayer)) {
                case PEACE: case CEASE_FIRE:
                    delta = Tension.Level.HATEFUL.getLimit()
                        - bad.getTension(serverPlayer).getValue();
                    break;
                case WAR: default:
                    delta = 0;
                    break;
                }
                cs.addMessage(See.only(serverPlayer),
                    new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                     "declareIndependence.nativeHostile", bad)
                        .addStringTemplate("%nation%", bad.getNationLabel()));
                if (delta != 0) bad.csModifyTension(serverPlayer, delta, cs);
                Player.makeContact(bad, refPlayer);
                bad.csModifyTension(refPlayer,
                    -bad.getTension(refPlayer).getValue(), cs);
            }
        }

        // Make the mercenary force offer
        serverPlayer.csMercenaries(monarch.getMercenaryForce().getUnitList(),
            Monarch.MonarchAction.HESSIAN_MERCENARIES, random, cs);
        
        // Pity to have to update such a heavy object as the player,
        // but we do this, at most, once per player.  Other players
        // only need a partial player update and the stance change.
        // Put the stance change after the name change so that the
        // other players see the new nation name declaring war.  The
        // REF is hardwired to declare war on rebels so there is no
        // need to adjust its stance or tension.
        cs.addPartial(See.all().except(serverPlayer), serverPlayer,
                      "playerType", "independentNationName", "newLandName");
        cs.addMessage(See.all().except(serverPlayer),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "declareIndependence.announce",
                             serverPlayer)
                .addStringTemplate("%oldNation%", oldNation)
                .addStringTemplate("%newNation%", serverPlayer.getNationLabel())
                .add("%ruler%", serverPlayer.getRulerNameKey()));
        cs.add(See.only(serverPlayer), serverPlayer);
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Now that everything is ready, we can dispose of Europe.
        cs.addRemove(See.only(serverPlayer), null, europe);//-vis: not on map
        europe.dispose();
        // Do not clean up the Monarch, it contains the intervention force

        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Decline to investigate strange mounds.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param tile The {@code Tile} where the mounds are.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet declineMounds(ServerPlayer serverPlayer, Tile tile) {
        tile.cacheUnseen();//+til
        tile.removeLostCityRumour();//-til

        // Others might see rumour disappear
        ChangeSet cs = new ChangeSet();
        cs.add(See.perhaps(), tile);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Delete a trade route.
     *
     * @param serverPlayer The {@code ServerPlayer} to delete a trade
     *     route for.
     * @param tradeRoute The {@code TradeRoute} to delete.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet deleteTradeRoute(ServerPlayer serverPlayer,
                                      TradeRoute tradeRoute) {
        List<Unit> dropped = serverPlayer.removeTradeRoute(tradeRoute);

        // Trade route change is entirely internal
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), serverPlayer); // FIXME: big update
        if (!dropped.isEmpty()) cs.add(See.only(serverPlayer), dropped);
        return cs;
    }


    /**
     * Deliver gift to settlement.
     * Note that this includes both European and native gifts.
     *
     * @param serverPlayer The {@code ServerPlayer} that is delivering.
     * @param unit The {@code Unit} that is delivering.
     * @param settlement The {@code Settlement} to deliver to.
     * @param goods The {@code Goods} to deliver.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet deliverGiftToSettlement(ServerPlayer serverPlayer,
                                             Unit unit, Settlement settlement,
                                             Goods goods) {
        NativeTradeSession session
            = Session.lookup(NativeTradeSession.class, unit, settlement);
        if (session == null) {
            return serverPlayer.clientError("Trying to deliver gift without opening a session");
        }
        NativeTrade nt = session.getNativeTrade();
        if (!nt.getGift()) {
            return serverPlayer.clientError("Trying to deliver gift in a session where gift giving is not allowed: " + unit + " " + settlement + " " + session);
        }

        ChangeSet cs = new ChangeSet();
        Tile tile = settlement.getTile();
        moveGoods(unit, goods.getType(), goods.getAmount(), settlement);
        cs.add(See.perhaps(), unit);
        if (settlement instanceof ServerIndianSettlement) {
            ServerIndianSettlement sis = (ServerIndianSettlement)settlement;
            csVisit(serverPlayer, sis, 0, cs);
            sis.csModifyAlarm(serverPlayer, -sis.getPriceToBuy(goods) / 50,
                              true, cs);
            sis.updateWantedGoods();
            tile.updateIndianSettlement(serverPlayer);
            cs.add(See.only(serverPlayer), tile);
        }
        nt.setGift(true);

        // Inform the receiver of the gift.
        ModelMessage m = new ModelMessage(MessageType.GIFT_GOODS,
                                          "deliverGift.goods",
                                          settlement, goods.getType())
            .addStringTemplate("%player%", serverPlayer.getNationLabel())
            .addNamed("%type%", goods)
            .addAmount("%amount%", goods.getAmount())
            .addName("%settlement%", settlement.getName());
        cs.addMessage(See.only(serverPlayer), m);
        ServerPlayer receiver = (ServerPlayer) settlement.getOwner();
        if (receiver.isConnected() && settlement instanceof Colony) {
            cs.add(See.only(receiver), unit);
            cs.add(See.only(receiver), settlement);
            cs.addMessage(See.only(receiver), m);
        }
        logger.info("Gift delivered by unit: " + unit.getId()
                    + " to settlement: " + settlement.getName());

        // Others can see unit capacity, receiver gets it own items.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Demand a tribute from a native settlement.
     *
     * FIXME: Move TURNS_PER_TRIBUTE magic number to the spec.
     *
     * @param serverPlayer The {@code ServerPlayer} demanding the tribute.
     * @param unit The {@code Unit} that is demanding the tribute.
     * @param is The {@code IndianSettlement} demanded of.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet demandTribute(ServerPlayer serverPlayer, Unit unit,
                                   IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        final int TURNS_PER_TRIBUTE = 5;

        csVisit(serverPlayer, is, 0, cs);

        Player indianPlayer = is.getOwner();
        int gold = 0;
        int year = getGame().getTurn().getNumber();
        RandomRange gifts = is.getType().getGifts(unit);
        if (is.getLastTribute() + TURNS_PER_TRIBUTE < year
            && gifts != null) {
            switch (indianPlayer.getTension(serverPlayer).getLevel()) {
            case HAPPY: case CONTENT:
                gold = Math.min(gifts.getAmount("Tribute", random, true) / 10,
                                100);
                break;
            case DISPLEASED:
                gold = Math.min(gifts.getAmount("Tribute", random, true) / 20,
                                100);
                break;
            case ANGRY: case HATEFUL:
            default:
                gold = 0; // No tribute for you.
                break;
            }
        }

        // Increase tension whether we paid or not.  Apply tension
        // directly to the settlement and let propagation work.
        ((ServerIndianSettlement)is).csModifyAlarm(serverPlayer,
            Tension.TENSION_ADD_NORMAL, true, cs);
        is.setLastTribute(year);
        ModelMessage m;
        if (gold > 0) {
            indianPlayer.modifyGold(-gold);
            serverPlayer.modifyGold(gold);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold", "score");
            m = new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeAgree",
                                 unit, is)
                .addAmount("%amount%", gold);
        } else {
            m = new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "scoutSettlement.tributeDisagree",
                                 unit, is);
        }
        cs.addMessage(See.only(serverPlayer), m);
        final Tile tile = is.getTile();
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");

        // Do not update others, this is all private.
        return cs;
    }


    /**
     * Denounce an existing mission.
     *
     * @param serverPlayer The {@code ServerPlayer} that is denouncing.
     * @param unit The {@code Unit} denouncing.
     * @param is The {@code IndianSettlement} containing the mission
     *     to denounce.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet denounceMission(ServerPlayer serverPlayer, Unit unit,
                                     IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        csVisit(serverPlayer, is, 0, cs);

        // Determine result
        Unit missionary = is.getMissionary();
        if (missionary == null) {
            return serverPlayer.clientError("Denouncing null missionary");
        }
        ServerPlayer enemy = (ServerPlayer)missionary.getOwner();
        double denounce = randomDouble(logger, "Denounce base", random)
            * enemy.getImmigration() / (serverPlayer.getImmigration() + 1);
        if (missionary.hasAbility(Ability.EXPERT_MISSIONARY)) {
            denounce += 0.2;
        }
        if (unit.hasAbility(Ability.EXPERT_MISSIONARY)) {
            denounce -= 0.2;
        }

        if (denounce < 0.5) { // Success, remove old mission and establish ours
            return establishMission(serverPlayer, unit, is);
        }

        // Denounce failed
        final Player owner = is.getOwner();
        cs.add(See.only(serverPlayer), is);
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission.noDenounce",
                             serverPlayer, unit)
                .addStringTemplate("%nation%", owner.getNationLabel()));
        cs.addMessage(See.only(enemy),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission.enemyDenounce",
                             enemy, is)
                .addStringTemplate("%enemy%", serverPlayer.getNationLabel())
                .addStringTemplate("%settlement%",
                    is.getLocationLabelFor(enemy))
                .addStringTemplate("%nation%", owner.getNationLabel()));
        cs.add(See.perhaps().always(serverPlayer),
               (FreeColGameObject)unit.getLocation());
        cs.addRemove(See.perhaps().always(serverPlayer),
                     unit.getLocation(), unit);//-vis(serverPlayer)
        unit.dispose();
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Others can see missionary disappear
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Diplomacy.
     *
     * @param serverPlayer The {@code ServerPlayer} that is trading.
     * @param ourUnit The {@code Unit} that is trading.
     * @param otherColony The {@code Colony} to trade with.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet diplomacy(ServerPlayer serverPlayer, Unit ourUnit,
                               Colony otherColony, DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();
        TradeStatus status = agreement.getStatus();
        DiplomacySession session
            = Session.lookup(DiplomacySession.class,
                             ourUnit, otherColony);
        if (session == null) {
            if (status != TradeStatus.PROPOSE_TRADE) {
                return serverPlayer.clientError("Mission uc-diplomacy session for "
                    + ourUnit.getId() + "/" + otherColony.getId()
                    + " with " + agreement);
            }
            session = new DiplomacySession(ourUnit, otherColony);
        }
        ServerPlayer otherPlayer = (ServerPlayer)otherColony.getOwner();
        if (csDiplomacySession(serverPlayer, otherPlayer, agreement, session,
                new DiplomacyMessage(otherColony, ourUnit, agreement), cs)) {
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new DiplomacyMessage(ourUnit, otherColony, 
                                     session.getAgreement()));
        }
        return cs;
    }

    /**
     * Diplomacy.
     *
     * @param serverPlayer The {@code ServerPlayer} that is trading.
     * @param ourColony Our {@code Colony}.
     * @param otherUnit The other {@code Unit} that is trading.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet diplomacy(ServerPlayer serverPlayer, Colony ourColony,
                               Unit otherUnit, DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();
        DiplomacySession session = Session.lookup(DiplomacySession.class,
            otherUnit, ourColony);
        if (session == null) {
            return serverPlayer.clientError("Mission cu-diplomacy session for "
                + otherUnit.getId() + "/" + ourColony.getId()
                + " with " + agreement);
        }
        ServerPlayer otherPlayer = (ServerPlayer)otherUnit.getOwner();
        if (csDiplomacySession(serverPlayer, otherPlayer, agreement, session,
                new DiplomacyMessage(otherUnit, ourColony, agreement), cs)) {
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new DiplomacyMessage(ourColony, otherUnit,
                                     session.getAgreement()));
        }
        return cs;
    }


    /**
     * Disband a unit.
     *
     * @param serverPlayer The owner of the unit.
     * @param unit The {@code Unit} to disband.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet disbandUnit(ServerPlayer serverPlayer, Unit unit) {
        ChangeSet cs = new ChangeSet();

        // Dispose of the unit.
        cs.add(See.perhaps().always(serverPlayer),
               (FreeColGameObject)unit.getLocation());
        cs.addRemove(See.perhaps().always(serverPlayer),
                     unit.getLocation(), unit);//-vis(serverPlayer)
        unit.dispose();
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // Others can see the unit removal and the space it leaves.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Disembark unit from a carrier.
     *
     * @param serverPlayer The {@code ServerPlayer} whose unit is
     *                     embarking.
     * @param serverUnit The {@code ServerUnit} that is disembarking.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet disembarkUnit(ServerPlayer serverPlayer,
                                   ServerUnit serverUnit) {
        if (serverUnit.isNaval()) {
            return serverPlayer.clientError("Naval unit " + serverUnit.getId()
                + " can not disembark.");
        }
        Unit carrier = serverUnit.getCarrier();
        if (carrier == null) {
            return serverPlayer.clientError("Unit " + serverUnit.getId()
                + " is not embarked.");
        }

        ChangeSet cs = new ChangeSet();

        Location newLocation = carrier.getLocation();
        List<Tile> newTiles = (newLocation.getTile() == null) ? null
            : serverUnit.collectNewTiles(newLocation.getTile());
        serverUnit.setLocation(newLocation);//-vis(serverPlayer)
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
        serverUnit.setMovesLeft(0); // In Col1 disembark consumes whole move.
        cs.add(See.perhaps(), (FreeColGameObject)newLocation);
        if (newTiles != null) {
            serverPlayer.csSeeNewTiles(newTiles, cs);
        }

        // Others can (potentially) see the location.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Embark a unit onto a carrier.
     * Checking that the locations are appropriate is not done here.
     *
     * @param serverPlayer The {@code ServerPlayer} embarking.
     * @param serverUnit The {@code ServerUnit} that is embarking.
     * @param carrier The {@code Unit} to embark onto.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet embarkUnit(ServerPlayer serverPlayer,
                                ServerUnit serverUnit, Unit carrier) {
        if (serverUnit.isNaval()) {
            return serverPlayer.clientError("Naval unit " + serverUnit.getId()
                + " can not embark.");
        }
        UnitLocation.NoAddReason reason = carrier.getNoAddReason(serverUnit);
        if (reason != UnitLocation.NoAddReason.NONE) {
            return serverPlayer.clientError("Carrier: " + carrier.getId()
                + " can not carry " + serverUnit.getId() + ": " + reason);
        }

        ChangeSet cs = new ChangeSet();
        serverUnit.csEmbark(carrier, cs);

        // Others might see the unit disappear, or the carrier capacity.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * A unit migrates from Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} whose unit it will be.
     * @param slot The slot within {@code Europe} to select the unit from.
     * @param type The type of migration occurring.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet emigrate(ServerPlayer serverPlayer, int slot,
                              MigrationType type) {
        ChangeSet cs = new ChangeSet();
        serverPlayer.csEmigrate(slot, type, random, cs);

        // Do not update others, emigration is private.
        return cs;
    }


    /**
     * Ends the turn of the given player.
     *
     * Note: sends messages to other players.
     *
     * @param serverPlayer The {@code ServerPlayer} to end the turn of.
     * @return A {@code ChangeSet} encapsulating the end of turn changes.
     */
    public ChangeSet endTurn(ServerPlayer serverPlayer) {
        final FreeColServer freeColServer = getFreeColServer();
        final ServerGame game = getGame();
        final ServerPlayer winner = (ServerPlayer)game.checkForWinner();

        ServerPlayer current = (ServerPlayer)game.getCurrentPlayer();
        if (serverPlayer != current) {
            throw new IllegalArgumentException("It is not "
                + serverPlayer.getName() + "'s turn, it is "
                + ((current == null) ? "noone" : current.getName()) + "'s!");
        }

        for (;;) {
            logger.finest("Ending turn for " + current.getName());
            current.clearModelMessages();

            // Check for new turn
            ChangeSet cs = new ChangeSet();
            if (game.isNextPlayerInNewTurn()) {
                ChangeSet next = new ChangeSet();
                game.csNextTurn(next);
                game.sendToAll(next);

                LogBuilder lb = new LogBuilder(512);
                lb.add("New turn ", game.getTurn(), " for ");
                game.csNewTurn(random, lb, cs);
                lb.shrink(", ");
                lb.log(logger, Level.FINEST);
                if (debugOnlyAITurns > 0) {
                    if (--debugOnlyAITurns <= 0) {
                        // If this was a debug run, complete it.  This will
                        // signal the client to save and quit at the next
                        // suitable opportunity.
                        FreeColDebugger.signalEndDebugRun();
                    }
                }
            }

            if ((current = (ServerPlayer)game.getNextPlayer()) == null) {
                // "can not happen"
                return serverPlayer.clientError("Can not get next player");
            }

            // Remove dead players and retry
            switch (current.checkForDeath()) {
            case ServerPlayer.IS_DEAD:
                current.csWithdraw(cs);
                logger.info("For " + serverPlayer.getSuffix()
                    + ", " + current.getNation() + " is dead.");
                game.sendToAll(cs, current);
                continue;
            case ServerPlayer.IS_ALIVE:
                if (current.isREF() && current.checkForREFDefeat()) {
                    for (Player p : current.getRebels()) {
                        csGiveIndependence(current, (ServerPlayer)p, cs);
                    }
                    current.csWithdraw(cs);
                    logger.info(current.getNation() + " is defeated.");
                    game.sendToAll(cs, current);
                    continue;
                }
                break;
            default: // Need to autorecruit a unit to keep alive.
                current.csEmigrate(0, MigrationType.SURVIVAL, random, cs);
                break;
            }
            // Are there humans left?
            // FIXME: see if this can be relaxed so we can run large
            // AI-only simulations.
            boolean onlyAI = all(game.getConnectedPlayers(), Player::isAI);
            if (onlyAI) {
                logger.info("No human player left.");
                if (debugOnlyAITurns > 0) { // Complete debug runs
                    FreeColDebugger.signalEndDebugRun();
                }
                game.setCurrentPlayer(null);

                cs.addTrivial(See.all(), "gameEnded",
                              ChangePriority.CHANGE_NORMAL);
                game.sendToOthers(serverPlayer, cs);
                return cs;
            }

            // Has the player won?
            // Do not end single player games where an AI has won,
            // that would stop revenge mode.
            if (winner == current
                && !(freeColServer.getSinglePlayer() && winner.isAI())) {
                boolean highScore = !winner.isAI()
                    && HighScore.newHighScore(winner);
                cs.addTrivial(See.all(), "gameEnded",
                              ChangePriority.CHANGE_NORMAL,
                              "highScore", String.valueOf(highScore),
                              "winner", winner.getId());
            }

            // Do "new turn"-like actions that need to wait until right
            // before the player is about to move.
            game.setCurrentPlayer(current);
            if (current.isREF() && current.getEntryLocation() == null) {
                // Initialize this newly created REF
                // If the teleportREF option is enabled, teleport it in.
                REFAIPlayer refAIPlayer = (REFAIPlayer)freeColServer
                    .getAIPlayer(current);
                boolean teleport = game.getSpecification()
                    .getBoolean(GameOptions.TELEPORT_REF);
                if (refAIPlayer.initialize(teleport)) {
                    csLaunchREF(current, teleport, cs);
                } else {
                    logger.severe("REF failed to initialize.");
                }
            }
            current.csStartTurn(random, cs);

            cs.addTrivial(See.all(), "setCurrentPlayer",
                          ChangePriority.CHANGE_LATE,
                          "player", current.getId());
            if (current.getPlayerType() == PlayerType.COLONIAL) {
                Monarch monarch = current.getMonarch();
                MonarchAction action = null;
                if (debugMonarchAction != null
                    && current == debugMonarchPlayer) {
                    action = debugMonarchAction;
                    debugMonarchAction = null;
                    debugMonarchPlayer = null;
                    logger.finest("Debug monarch action: " + action);
                } else {
                    action = RandomChoice.getWeightedRandom(logger,
                            "Choose monarch action",
                        monarch.getActionChoices(), random);
                }
                if (action != null) {
                    if (monarch.actionIsValid(action)) {
                        logger.finest("Monarch action: " + action);
                        csMonarchAction(current, action, cs);
                    } else {
                        logger.finest("Skipping invalid monarch action: "
                            + action);
                    }
                }
            }

            // Prepare to update, with current player last so that it
            // does not immediately start moving and cause further
            // changes which conflict with these updates.
            List<ServerPlayer> players = game.getConnectedPlayers(current);
            players.add(current);

            // If this is a debug run, update everyone and continue.
            boolean debugSkip = !current.isAI()
                && freeColServer.getSinglePlayer()
                && debugOnlyAITurns > 0;
            if (debugSkip) {
                game.sendToList(players, cs);
                continue;
            }

            // Flush accumulated changes, returning to serverPlayer.
            players.remove(serverPlayer);
            game.sendToList(players, cs);
            return cs;
        }
    }


    /**
     * Enters revenge mode against those evil AIs.
     *
     * @param serverPlayer The {@code ServerPlayer} entering revenge mode.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet enterRevengeMode(ServerPlayer serverPlayer) {
        if (!getFreeColServer().getSinglePlayer()) {
            return serverPlayer.clientError("Can not enter revenge mode,"
                + " as this is not a single player game.");
        }
        Game game = getGame();
        List<UnitType> undeads = game.getSpecification()
            .getUnitTypesWithAbility(Ability.UNDEAD);
        List<UnitType> navalUnits = new ArrayList<>();
        List<UnitType> landUnits = new ArrayList<>();
        for (UnitType undead : undeads) {
            if (undead.hasAbility(Ability.NAVAL_UNIT)) {
                navalUnits.add(undead);
            } else if (undead.hasAbility(Ability.MULTIPLE_ATTACKS)) {
                landUnits.add(undead);
            }
        }
        if (navalUnits.isEmpty() || landUnits.isEmpty()) {
            return serverPlayer.clientError("Can not enter revenge mode,"
                + " because we can not find the undead units.");
        }

        ChangeSet cs = new ChangeSet();
        UnitType navalType = getRandomMember(logger, "Choose undead navy",
                                             navalUnits, random);
        Tile start = ((Tile)serverPlayer.getEntryLocation())
            .getSafeTile(serverPlayer, random);
        Unit theFlyingDutchman
            = new ServerUnit(game, start, serverPlayer,
                             navalType);//-vis(serverPlayer)
        UnitType landType = getRandomMember(logger, "Choose undead army",
                                            landUnits, random);
        new ServerUnit(game, theFlyingDutchman, serverPlayer, landType);//-vis
        serverPlayer.setDead(false);
        serverPlayer.changePlayerType(PlayerType.UNDEAD);
        serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)

        // No one likes the undead.
        for (Player p : transform(game.getLivePlayers(serverPlayer),
                                  p2 -> serverPlayer.hasContacted(p2))) {
            serverPlayer.csChangeStance(Stance.WAR, (ServerPlayer)p, true, cs);
        }

        // Revenge begins
        game.setCurrentPlayer(serverPlayer);
        cs.addTrivial(See.all(), "setCurrentPlayer",
                      ChangePriority.CHANGE_LATE,
                      "player", serverPlayer.getId());

        // Others can tell something has happened to the player,
        // and possibly see the units.
        cs.add(See.all(), serverPlayer);
        cs.add(See.perhaps(), start);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Equip a unit for a specific role.
     * Currently the unit is either in Europe or in a settlement.
     * Might one day allow the unit to be on a tile co-located with
     * an equipment-bearing wagon.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to equip.
     * @param role The {@code Role} to equip for.
     * @param roleCount The role count.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet equipForRole(ServerPlayer serverPlayer, Unit unit,
                                  Role role, int roleCount) {
        ChangeSet cs = new ChangeSet();
        boolean ret = false;
        if (unit.isInEurope()) {
            ServerEurope serverEurope = (ServerEurope)serverPlayer.getEurope();
            ret = serverEurope.csEquipForRole(unit, role, roleCount,
                                              random, cs);
        } else if (unit.getColony() != null) {
            ServerColony serverColony = (ServerColony)unit.getColony();
            ret = serverColony.csEquipForRole(unit, role, roleCount,
                                              random, cs);
        } else if (unit.getIndianSettlement() != null) {
            ServerIndianSettlement sis
                = (ServerIndianSettlement)unit.getIndianSettlement();
            ret = sis.csEquipForRole(unit, role, roleCount, random, cs);
        } else {
            return serverPlayer.clientError("Unsuitable equip location for: "
                + unit.getId());
        }
        if (!ret) return null;

        if (unit.getInitialMovesLeft() != unit.getMovesLeft()) {
            unit.setMovesLeft(0);
        }
        Unit carrier = unit.getCarrier();
        if (carrier != null
            && carrier.getInitialMovesLeft() != carrier.getMovesLeft()
            && carrier.getMovesLeft() != 0) {
            carrier.setMovesLeft(0);
        }
        return cs;
    }


    /**
     * Establish a new mission.
     *
     * @param serverPlayer The {@code ServerPlayer} that is establishing.
     * @param unit The missionary {@code Unit}.
     * @param is The {@code IndianSettlement} to establish at.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet establishMission(ServerPlayer serverPlayer, Unit unit,
                                      IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        csVisit(serverPlayer, is, 0, cs);

        // Result depends on tension wrt this settlement.
        // Establish if at least not angry.
        final Tension tension = is.getAlarm(serverPlayer);
        switch (tension.getLevel()) {
        case HATEFUL: case ANGRY:
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject)unit.getLocation());
            cs.addRemove(See.perhaps().always(serverPlayer),
                         unit.getLocation(), unit);//-vis(serverPlayer)
            unit.dispose();
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            break;

        case HAPPY: case CONTENT: case DISPLEASED:
            ServerIndianSettlement sis = (ServerIndianSettlement)is;
            if (is.hasMissionary()) sis.csKillMissionary(false, cs);

            // Always show the tile the unit was on
            cs.add(See.perhaps().always(serverPlayer), unit.getTile());
            
            sis.csChangeMissionary(unit, cs);//+vis(serverPlayer)
            break;
        }

        // Add the descriptive message.
        final StringTemplate nation = is.getOwner().getNationLabel();
        cs.addMessage(See.only(serverPlayer),
            new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                             "indianSettlement.mission." + tension.getKey(),
                             serverPlayer, unit)
                .addStringTemplate("%nation%", nation));

        // Others can see missionary disappear and settlement acquire
        // mission.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Handle first contact between European players.
     *
     * @param serverPlayer The {@code ServerPlayer} making contact.
     * @param ourUnit Our {@code Unit}.
     * @param otherUnit The other {@code unit}.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet europeanFirstContact(ServerPlayer serverPlayer,
                                          Unit ourUnit, Unit otherUnit,
                                          DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();
        DiplomacySession session = Session.lookup(DiplomacySession.class, 
                                                  ourUnit, otherUnit);
        if (session == null) {
            session = Session.lookup(DiplomacySession.class,
                                     otherUnit, ourUnit);
        }
        if (session == null) {
            if (agreement.getStatus() != TradeStatus.PROPOSE_TRADE) {
                return serverPlayer.clientError("Missing uu1-diplomacy session for "
                    + ourUnit.getId() + "," + otherUnit.getId()
                    + " with " + agreement);
            }
            session = new DiplomacySession(ourUnit, otherUnit);
            ourUnit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), ourUnit, "movesLeft");
        }
        ServerPlayer otherPlayer = (ServerPlayer)otherUnit.getOwner();
        if (csDiplomacySession(serverPlayer, otherPlayer,
                agreement, session,
                new DiplomacyMessage(otherUnit, ourUnit, agreement), cs)) {
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new DiplomacyMessage(ourUnit, otherUnit, 
                                     session.getAgreement()));
        }
        return cs;
    }

    /**
     * Handle first contact between European players.
     *
     * @param serverPlayer The {@code ServerPlayer} making contact.
     * @param ourUnit Our {@code Unit}.
     * @param otherColony The other {@code Colony}.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet europeanFirstContact(ServerPlayer serverPlayer,
                                          Unit ourUnit, Colony otherColony,
                                          DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();
        DiplomacySession session = Session.lookup(DiplomacySession.class,
                                                  ourUnit, otherColony);
        if (session == null) {
            if (agreement.getStatus() != TradeStatus.PROPOSE_TRADE) {
                return serverPlayer.clientError("Missing uc1-diplomacy session for "
                    + ourUnit.getId() + "," + otherColony.getId()
                    + " with " + agreement);
            }
            session = new DiplomacySession(ourUnit, otherColony);
            ourUnit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), ourUnit, "movesLeft");
        }
        ServerPlayer otherPlayer = (ServerPlayer)otherColony.getOwner();
        if (csDiplomacySession(serverPlayer, otherPlayer,
                agreement, session,
                new DiplomacyMessage(otherColony, ourUnit, agreement), cs)) {
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new DiplomacyMessage(ourUnit, otherColony, 
                                     session.getAgreement()));
        }
        return cs;
    }

    /**
     * Handle first contact between European players.
     *
     * @param serverPlayer The {@code ServerPlayer} making contact.
     * @param ourColony Our {@code Colony}.
     * @param otherUnit The other {@code Unit}.
     * @param agreement The {@code DiplomaticTrade} to consider.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet europeanFirstContact(ServerPlayer serverPlayer,
                                          Colony ourColony, Unit otherUnit,
                                          DiplomaticTrade agreement) {
        ChangeSet cs = new ChangeSet();
        DiplomacySession session = Session.lookup(DiplomacySession.class,
                                                  otherUnit, ourColony);
        if (session == null) {
            return serverPlayer.clientError("Missing cu1-diplomacy session for "
                + ourColony.getId() + "," + otherUnit.getId()
                + " with " + agreement);
        }
        ServerPlayer otherPlayer = (ServerPlayer)otherUnit.getOwner();
        if (csDiplomacySession(serverPlayer, otherPlayer,
                agreement, session,
                new DiplomacyMessage(otherUnit, ourColony, agreement), cs)) {
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                new DiplomacyMessage(ourColony, otherUnit,
                                     session.getAgreement()));
        }
        return cs;
    }


    /**
     * Get the goods for sale in a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is enquiring.
     * @param unit The {@code Unit} that is trading.
     * @param settlement The {@code Settlement} that is trading.
     * @return A {@code GoodsForSaleMessage} encapsulating this action,
     *     or an error message on failure.
     */
    public DOMMessage getGoodsForSale(ServerPlayer serverPlayer,
        Unit unit, Settlement settlement) {
        List<Goods> sellGoods = null;

        if (settlement instanceof IndianSettlement) {
            final Game game = serverPlayer.getGame();
            int goodsSellNumber = Math.max(1, game.getSpecification()
                .getInteger(GameOptions.SETTLEMENT_NUMBER_OF_GOODS_TO_SELL));
            IndianSettlement indianSettlement = (IndianSettlement) settlement;
            AIPlayer aiPlayer = getFreeColServer()
                .getAIPlayer(indianSettlement.getOwner());
            sellGoods = indianSettlement.getSellGoods(unit);
            if (sellGoods.size() > goodsSellNumber) {
                sellGoods = sellGoods.subList(0, goodsSellNumber);
            }
            for (Goods goods : sellGoods) {
                aiPlayer.registerSellGoods(goods);
            }
        } else { // Colony might be supported one day?
            return new ErrorMessage("Bogus settlement");
        }
        return new GoodsForSaleMessage(unit, settlement, sellGoods);
    }


    /**
     * Gets the list of high scores.
     *
     * @return A list of {@code HighScore}.
     */
    public List<HighScore> getHighScores() {
        return HighScore.loadHighScores();
    }


    /**
     * Incite a settlement against an enemy.
     *
     * @param serverPlayer The {@code ServerPlayer} that is inciting.
     * @param unit The missionary {@code Unit} inciting.
     * @param is The {@code IndianSettlement} to incite.
     * @param enemy The {@code ServerPlayer} to be incited against.
     * @param gold The amount of gold in the bribe.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet incite(ServerPlayer serverPlayer, Unit unit,
        IndianSettlement is, ServerPlayer enemy, int gold) {
        ChangeSet cs = new ChangeSet();

        Tile tile = is.getTile();
        csVisit(serverPlayer, is, 0, cs);
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);

        // How much gold will be needed?
        ServerPlayer enemyPlayer = enemy;
        ServerPlayer nativePlayer = (ServerPlayer)is.getOwner();
        int payingValue = nativePlayer.getTension(serverPlayer).getValue();
        int targetValue = nativePlayer.getTension(enemyPlayer).getValue();
        int goldToPay = (payingValue > targetValue) ? 10000 : 5000;
        goldToPay += 20 * (payingValue - targetValue);
        goldToPay = Math.max(goldToPay, 650);

        // Try to incite?
        if (gold < 0) { // Initial inquiry
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_NORMAL,
                new InciteMessage(unit, is, enemy, goldToPay));
        } else if (gold < goldToPay || !serverPlayer.checkGold(gold)) {
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "missionarySettlement.inciteGoldFail",
                                 serverPlayer, is)
                    .addStringTemplate("%player%",
                        enemyPlayer.getNationLabel())
                    .addAmount("%amount%", goldToPay));
            cs.addAttribute(See.only(serverPlayer), "gold", "0");
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        } else {
            // Success.  Raise the tension for the native player with respect
            // to the European player.  Let resulting stance changes happen
            // naturally in the AI player turn/s.
            nativePlayer.csModifyTension(enemyPlayer,
                Tension.WAR_MODIFIER, cs);//+til
            enemyPlayer.csModifyTension(serverPlayer,
                Tension.TENSION_ADD_WAR_INCITER, cs);//+til
            cs.addAttribute(See.only(serverPlayer),
                            "gold", Integer.toString(gold));
            serverPlayer.modifyGold(-gold);
            nativePlayer.modifyGold(gold);
            cs.addMessage(See.only(serverPlayer),
                new ModelMessage(MessageType.FOREIGN_DIPLOMACY,
                                 "missionarySettlement.inciteSuccess",
                                 nativePlayer)
                .addStringTemplate("%native%", nativePlayer.getNationLabel())
                    .addStringTemplate("%enemy%", enemy.getNationLabel()));
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
            unit.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        }

        // Others might include enemy.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Indians making demands of a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that is demanding.
     * @param unit The {@code Unit} making the demands.
     * @param colony The {@code Colony} that is demanded of.
     * @param type The {@code GoodsType} being demanded, null
     *     implies gold.
     * @param amount The amount of goods/gold being demanded.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet indianDemand(final ServerPlayer serverPlayer, Unit unit,
                                  Colony colony, GoodsType type, int amount) {
        final Game game = getGame();
        final Specification spec = game.getSpecification();
        ServerPlayer victim = (ServerPlayer) colony.getOwner();
        int difficulty = spec.getInteger(GameOptions.NATIVE_DEMANDS);
        ChangeSet cs = new ChangeSet();

        DOMMessage reply = askTimeout(victim,
            new IndianDemandMessage(unit, colony, type, amount));
        boolean result = (reply instanceof IndianDemandMessage)
            ? ((IndianDemandMessage)reply).getResult()
            : false;
        logger.info(serverPlayer.getName() + " unit " + unit
            + " demands " + amount + " " + ((type == null) ? "gold" : type)
            + " from " + colony.getName() + " accepted: " + result);

        IndianDemandMessage message = new IndianDemandMessage(unit, colony,
                                                              type, amount);
        message.setResult(result);
        cs.add(See.only(serverPlayer), ChangePriority.CHANGE_NORMAL, message);
        if (result) {
            if (type == null) {
                victim.modifyGold(-amount);
                serverPlayer.modifyGold(amount);
                cs.addPartial(See.only(victim), victim, "gold");
                //cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
            } else {
                GoodsContainer colonyContainer = colony.getGoodsContainer();
                colonyContainer.saveState();
                GoodsContainer unitContainer = unit.getGoodsContainer();
                unitContainer.saveState();
                moveGoods(colony, type, amount, unit);
                cs.add(See.only(victim), colonyContainer);
                //cs.add(See.only(serverPlayer), unitContainer);
            }
            int tension = -(5 - difficulty) * 50;
            ServerIndianSettlement sis = (ServerIndianSettlement)
                unit.getHomeIndianSettlement();
            if (sis == null) {
                serverPlayer.csModifyTension(victim, tension, cs);
            } else {
                sis.csModifyAlarm(victim, tension, true, cs);
            }
        }

        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Join a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} that is joining.
     * @param colony The {@code Colony} to join.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet joinColony(ServerPlayer serverPlayer, Unit unit,
                                Colony colony) {
        final Specification spec = getGame().getSpecification();
        ChangeSet cs = new ChangeSet();
        Set<Tile> ownedTiles = colony.getOwnedTiles();
        Tile tile = colony.getTile();

        // Join.
        tile.cacheUnseen();//+til
        unit.setLocation(colony);//-vis: safe/colony,-til
        unit.setMovesLeft(0);
        colony.equipForRole(unit, spec.getDefaultRole(), 0);

        // Update with colony tile, and tiles now owned.
        cs.add(See.only(serverPlayer), tile);
        for (Tile t : transform(tile.getSurroundingTiles(1, colony.getRadius()),
                t2 -> (t2.getOwningSettlement() == colony
                    && !ownedTiles.contains(t2)))) {
            cs.add(See.perhaps(), t);
        }

        // Others might see a tile ownership change.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Learn a skill at an IndianSettlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is learning.
     * @param unit The {@code Unit} that is learning.
     * @param is The {@code IndianSettlement} to learn from.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet learnFromIndianSettlement(ServerPlayer serverPlayer,
                                               Unit unit, IndianSettlement is) {
        // Sanity checks.
        final Specification spec = getGame().getSpecification();
        final UnitType skill = is.getLearnableSkill();
        if (skill == null) {
            return serverPlayer.clientError("No skill to learn at "
                + is.getName());
        }
        if (unit.getUnitChange(UnitChangeType.NATIVES, skill) == null) {
            return serverPlayer.clientError("Unit " + unit
                + " can not learn skill " + skill + " at " + is.getName());
        }

        // Try to learn
        ChangeSet cs = new ChangeSet();
        unit.setMovesLeft(0);
        csVisit(serverPlayer, is, 0, cs);
        switch (is.getAlarm(serverPlayer).getLevel()) {
        case HATEFUL: // Killed, might be visible to other players.
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject)unit.getLocation());
            cs.addRemove(See.perhaps().always(serverPlayer),
                         unit.getLocation(), unit);//-vis(serverPlayer)
            unit.dispose();
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            break;
        case ANGRY: // Learn nothing, not even a pet update
            cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
            break;
        default:
            // Teach the unit, and expend the skill if necessary.
            // Do a full information update as the unit is in the settlement.
            unit.changeType(skill);//-vis(serverPlayer)
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            cs.add(See.perhaps(), unit);
            if (!is.isCapital()
                && !(is.hasMissionary(serverPlayer)
                    && spec.getBoolean(GameOptions.ENHANCED_MISSIONARIES))) {
                is.setLearnableSkill(null);
            }
            break;
        }
        Tile tile = is.getTile();
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);

        // Others always see the unit, it may have died or been taught.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Load goods.
     *
     * @param serverPlayer The {@code ServerPlayer} that is loading.
     * @param loc The {@code Location} where the goods are.
     * @param goodsType The {@code GoodsType} to load.
     * @param amount The amount of goods to load.
     * @param carrier The {@code Unit} to load.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet loadGoods(ServerPlayer serverPlayer, Location loc,
                               GoodsType goodsType, int amount, Unit carrier) {
        if (loc instanceof Europe) {
            if (carrier.isInEurope()) {
                return buyGoods(serverPlayer, goodsType, amount, carrier);
            } else {
                return serverPlayer.clientError("Carrier not in Europe: " + loc);
            }
        }
        // All loading locations other than Europe are GoodsLocations
        if (!(loc instanceof GoodsLocation)) {
            return serverPlayer.clientError("Not a goods location: " + loc);
        }
        GoodsLocation gl = (GoodsLocation)loc;
        if (!carrier.isAtLocation(loc)) {
            return serverPlayer.clientError("Carrier not at location: " + loc);
        }
        if (carrier.getLoadableAmount(goodsType) < amount) {
            return serverPlayer.clientError("Too much goods");
        }
        if (gl.getGoodsCount(goodsType) < amount) {
            return serverPlayer.clientError("Not enough goods ("
                + gl.getGoodsCount(goodsType) + " < " + amount
                + " " + goodsType.getSuffix() + ") at " + gl);
        }

        ChangeSet cs = new ChangeSet();
        moveGoods(gl, goodsType, amount, carrier);
        logger.finest(Messages.message(loc.getLocationLabel())
            + " loaded " + amount + " " + goodsType.getSuffix()
            + " onto " + carrier);
        cs.add(See.only(serverPlayer), gl.getGoodsContainer());
        cs.add(See.only(serverPlayer), carrier.getGoodsContainer());
        if (carrier.getInitialMovesLeft() != carrier.getMovesLeft()) {
            carrier.setMovesLeft(0);
            cs.addPartial(See.only(serverPlayer), carrier, "movesLeft");
        }

        // Invisible in settlement
        return cs;
    }


    /**
     * Loot cargo.
     *
     * Note loser is passed by identifier, as by the time we get here
     * the unit may have been sunk.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the winner.
     * @param winner The {@code Unit} that looting.
     * @param loserId The object identifier of the {@code Unit}
     *     that is looted.
     * @param loot The {@code Goods} to loot.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet lootCargo(ServerPlayer serverPlayer, Unit winner,
                               String loserId, List<Goods> loot) {
        LootSession session = Session.lookup(LootSession.class,
                                             winner.getId(), loserId);
        if (session == null) {
            return serverPlayer.clientError("Bogus looting!");
        }
        if (!winner.hasSpaceLeft()) {
            return serverPlayer.clientError("No space to loot to: "
                + winner.getId());
        }

        ChangeSet cs = new ChangeSet();
        List<Goods> available = session.getCapture();
        if (loot == null) { // Initial inquiry
            cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
                   new LootCargoMessage(winner, loserId, available));
        } else {
            for (Goods g : loot) {
                if (!available.contains(g)) {
                    return serverPlayer.clientError("Invalid loot: " + g);
                }
                available.remove(g);
                if (!winner.canAdd(g)) {
                    return serverPlayer.clientError("Loot failed: " + g);
                }
                winner.add(g);
            }

            // Others can see cargo capacity change.
            session.complete(cs);
            cs.add(See.perhaps(), winner);
            getGame().sendToOthers(serverPlayer, cs);
        }
        return cs;
    }


    /**
     * Respond to a monarch action.
     *
     * @param serverPlayer The {@code ServerPlayer} that is to respond.
     * @param action The {@code MonarchAction} to respond to.
     * @param result The player response.
     * @return A {@code ChangeSet} containing the response.
     */
    public ChangeSet monarchAction(ServerPlayer serverPlayer,
                                   MonarchAction action, boolean result) {
        MonarchSession session = Session.lookup(MonarchSession.class,
            serverPlayer.getId(), "");
        if (session == null) {
            return serverPlayer.clientError("Bogus monarch action: " + action);
        } else if (action != session.getAction()) {
            return serverPlayer.clientError("Session action mismatch, "
                + session.getAction() + " expected: " + action);
        }

        ChangeSet cs = new ChangeSet();
        session.complete(result, cs);
        return cs;
    }


    /**
     * Move a unit.
     *
     * @param serverPlayer The {@code ServerPlayer} that is moving.
     * @param unit The {@code ServerUnit} to move.
     * @param newTile The {@code Tile} to move to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet move(ServerPlayer serverPlayer, ServerUnit unit,
                          Tile newTile) {
        ChangeSet cs = new ChangeSet();
        unit.csMove(newTile, random, cs);
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Move a unit across the high seas.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to move.
     * @param destination The {@code Location} to move to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet moveTo(ServerPlayer serverPlayer, Unit unit,
                            Location destination) {
        ChangeSet cs = new ChangeSet();
        HighSeas highSeas = serverPlayer.getHighSeas();
        Location current = unit.getDestination();
        boolean others = false; // Notify others?
        boolean invalid = false; // Not a highSeas move?

        if (!unit.getType().canMoveToHighSeas()) {
            invalid = true;
        } else if (destination instanceof Europe) {
            if (!highSeas.getDestinations().contains(destination)) {
                return serverPlayer.clientError("HighSeas does not connect to: "
                    + destination.getId());
            } else if (unit.getLocation() == highSeas) {
                if (!(current instanceof Europe)) {
                    // Changed direction
                    unit.setWorkLeft(unit.getSailTurns()
                        - unit.getWorkLeft() + 1);
                }
                unit.setDestination(destination);
                cs.add(See.only(serverPlayer), unit, highSeas);
            } else if (unit.hasTile()) {
                Tile tile = unit.getTile();
                unit.setEntryLocation(tile);
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);//-vis(serverPlayer)
                serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
                cs.addDisappear(serverPlayer, tile, unit);
                cs.add(See.only(serverPlayer), tile, highSeas);
                others = true;
            } else {
                invalid = true;
            }
        } else if (destination instanceof Map) {
            if (!highSeas.getDestinations().contains(destination)) {
                return serverPlayer.clientError("HighSeas does not connect to: "
                    + destination.getId());
            } else if (unit.getLocation() == highSeas) {
                if (current != destination && (current == null
                        || current.getTile() == null
                        || current.getTile().getMap() != destination)) {
                    // Changed direction
                    unit.setWorkLeft(unit.getSailTurns()
                        - unit.getWorkLeft() + 1);
                }
                unit.setDestination(destination);
                cs.add(See.only(serverPlayer), highSeas);
            } else if (unit.getLocation() instanceof Europe) {
                Europe europe = (Europe) unit.getLocation();
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);//-vis: safe!map
                cs.add(See.only(serverPlayer), europe, highSeas);
            } else {
                invalid = true;
            }
        } else if (destination instanceof Settlement) {
            Tile tile = destination.getTile();
            if (!highSeas.getDestinations().contains(tile.getMap())) {
                return serverPlayer.clientError("HighSeas does not connect to: "
                    + destination.getId());
            } else if (unit.getLocation() == highSeas) {
                // Direction is somewhat moot, so just reset.
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                cs.add(See.only(serverPlayer), highSeas);
            } else if (unit.getLocation() instanceof Europe) {
                Europe europe = (Europe) unit.getLocation();
                unit.setWorkLeft(unit.getSailTurns());
                unit.setDestination(destination);
                unit.setMovesLeft(0);
                unit.setLocation(highSeas);//-vis: safe!map
                cs.add(See.only(serverPlayer), europe, highSeas);
            } else {
                invalid = true;
            }
        } else {
            return serverPlayer.clientError("Bogus moveTo destination: "
                + destination.getId());
        }
        if (invalid) {
            return serverPlayer.clientError("Invalid moveTo: unit=" + unit.getId()
                + " from=" + unit.getLocation().getId()
                + " to=" + destination.getId());
        }

        if (others) getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Handle first contact between European and native player.
     *
     * Note that we check for a diplomacy session, but do only bother
     * in the case of tile!=null as that is the only possibility for
     * some benefit.
     *
     * @param serverPlayer The {@code ServerPlayer} making contact.
     * @param other The native {@code ServerPlayer} to contact.
     * @param tile A {@code Tile} on offer at first landing.
     * @param result Whether the initial peace treaty was accepted.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet nativeFirstContact(ServerPlayer serverPlayer,
                                        ServerPlayer other, Tile tile,
                                        boolean result) {
        ChangeSet cs = new ChangeSet();
        if (result) {
            if (tile != null) {
                Unit u = tile.getFirstUnit();
                Settlement s = tile.getOwningSettlement();
                DiplomacySession session = Session.lookup(DiplomacySession.class, u, s);
                if (session == null) {
                    return serverPlayer.clientError("No diplomacy in effect for: "
                        + tile.getId());
                }
                tile.cacheUnseen();//+til
                tile.changeOwnership(serverPlayer, null);//-til
                cs.add(See.perhaps(), tile);
            }
        } else {
            // Consider not accepting the treaty to be an insult and
            // ban missions.
            other.csModifyTension(serverPlayer,
                Tension.TENSION_ADD_MAJOR, cs);//+til
            other.addMissionBan(serverPlayer);
        }

        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * A native unit delivers its gift to a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that is delivering.
     * @param unit The {@code Unit} that is delivering.
     * @param colony The {@code Colony} to deliver to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet nativeGift(ServerPlayer serverPlayer,
                                Unit unit, Colony colony) {
        final Goods goods = first(unit.getGoods());
        if (goods == null) {
            return serverPlayer.clientError("No gift to deliver: "
                + unit.getId());
        }
        final ServerPlayer otherPlayer = (ServerPlayer)colony.getOwner();

        ChangeSet cs = new ChangeSet();
        moveGoods(unit, goods.getType(), goods.getAmount(), colony);
        cs.add(See.perhaps(), unit);

        // Inform the receiver of the gift.
        ModelMessage m = new ModelMessage(MessageType.GIFT_GOODS,
                                          "deliverGift.goods",
                                          colony, goods.getType())
            .addStringTemplate("%player%", serverPlayer.getNationLabel())
            .addNamed("%type%", goods)
            .addAmount("%amount%", goods.getAmount())
            .addName("%settlement%", colony.getName());
        cs.addMessage(See.only(otherPlayer), m);
        cs.add(See.only(otherPlayer), colony);
        logger.info("Gift delivered by unit: " + unit.getId()
            + " to colony " + colony.getName() + ": " + goods.toString());

        // Others might see unit capacity?
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Handle native trade sessions.
     *
     * @param serverPlayer The {@code ServerPlayer} that is trading.
     * @param action The {@code NativeTradeAction} to perform.
     * @param nt The {@code NativeTrade} underway.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet nativeTrade(ServerPlayer serverPlayer,
                                 NativeTradeAction action, NativeTrade nt) {
        final Unit unit = nt.getUnit();
        final IndianSettlement is = nt.getIndianSettlement();
        final ServerPlayer otherPlayer = (ServerPlayer)
            ((serverPlayer.owns(unit)) ? is.getOwner() : unit.getOwner());

        // Server view of the transaction is kept in the NativeTradeSession,
        // which is always updated with what the native player says, and
        // only partially update with what the human player says.
        NativeTradeSession session = Session.lookup(NativeTradeSession.class,
                                                    unit, is);
        if (action.isEuropean() != serverPlayer.isEuropean()) {
            return serverPlayer.clientError(((action.isEuropean())
                    ? "European" : "Native")
                + " player expected for " + action
                + ": " + serverPlayer.getSuffix());
        } else if (action == NativeTradeAction.OPEN && session != null) {
            return serverPlayer.clientError("Session already open for: " + nt);
        } else if (action != NativeTradeAction.OPEN && session == null) {
            return serverPlayer.clientError("No session found for: " + nt);
        }            

        ChangeSet cs = new ChangeSet();
        NativeTradeItem item;
        switch (action) {
        case OPEN: // Open a new session if possible
            if (unit.getMovesLeft() <= 0) {
                return serverPlayer.clientError("Unit " + unit.getId()
                    + " has no moves left.");
            }
            nt = new NativeTrade(nt.getUnit(), nt.getIndianSettlement());
            if (nt.getDone()) {
                // Trade can not happen, just outright NAK as invalid.
                cs.add(See.only(serverPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                       new NativeTradeMessage(NativeTradeAction.NAK_INVALID, nt));
            } else {
                // Trade can proceed.  Register a session and ask the natives
                // to price the goods.
                session = new NativeTradeSession(nt);
                nt.initialize();
                cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                       new NativeTradeMessage(action, nt));
            }
            break;

        case CLOSE: // Just close the session
            session.complete(cs);
            break;

        case BUY: // Check goods (not whole item, price might be wrong), forward
            item = nt.getItem();
            nt.mergeFrom(session.getNativeTrade());
            if (item == null) {
                return serverPlayer.clientError("Null purchase: " + nt);
            } else if (!nt.canBuy()) {
                return serverPlayer.clientError("Can not buy: " + nt);
            } else if (find(nt.getSettlementToUnit(),
                            item.goodsMatcher()) == null) {
                return serverPlayer.clientError("Item missing for "
                    + action + ": " + nt);
            }
            nt.setItem(item);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;

        case SELL: // Check goods, forward
            item = nt.getItem();
            nt.mergeFrom(session.getNativeTrade());
            if (item == null) {
                return serverPlayer.clientError("Null sale: " + nt);
            } else if (!nt.canSell()) {
                return serverPlayer.clientError("Can not sell: " + nt);
            } else if (find(nt.getUnitToSettlement(),
                            item.goodsMatcher()) == null) {
                return serverPlayer.clientError("Item missing for "
                    + action + ": " + nt);
            }
            nt.setItem(item);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;
            
        case GIFT: // Check goods, forward
            item = nt.getItem();
            nt.mergeFrom(session.getNativeTrade());
            if (item == null) {
                return serverPlayer.clientError("Null gift: " + nt);
            } else if (!nt.canGift()) {
                return serverPlayer.clientError("Can not gift: " + nt);
            } else if (find(nt.getUnitToSettlement(),
                            item.goodsMatcher()) == null) {
                return serverPlayer.clientError("Item missing for "
                    + action + ": " + nt);
            }
            nt.setItem(item);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;

        case ACK_OPEN: // Natives are prepared to trade, inform player
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            // Set unit moves to zero to avoid cheating.  If no
            // action is taken, the moves will be restored when
            // closing the session.
            unit.setMovesLeft(0);
            cs.addPartial(See.only(otherPlayer), unit, "movesLeft");
            break;

        case ACK_BUY_HAGGLE: case ACK_SELL_HAGGLE: // Successful haggle
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;
            
        case ACK_BUY: // Buy succeeded, update goods, inform player
            item = nt.getItem();
            csBuy(unit, item.getGoods(), item.getPrice(),
                  (ServerIndianSettlement)is, cs);
            nt.setBuy(false);
            nt.addToUnit(item);
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;

        case ACK_SELL: // Sell succeeded, update goods, inform player
            item = nt.getItem();
            csSell(unit, item.getGoods(), item.getPrice(),
                   (ServerIndianSettlement)is, cs);
            nt.setSell(false);
            nt.removeFromUnit(item);
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;

        case ACK_GIFT: // Gift succeeded, update goods, inform player
            item = nt.getItem();
            csGift(unit, item.getGoods(), item.getPrice(),
                   (ServerIndianSettlement)is, cs);
            nt.setGift(false);
            nt.removeFromUnit(item);
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;

        case NAK_GOODS: // Polite refusal of gift
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            break;

        case NAK_INVALID: case NAK_HOSTILE: case NAK_HAGGLE: // Fail, close
            session.getNativeTrade().mergeFrom(nt);
            cs.add(See.only(otherPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
                   new NativeTradeMessage(action, nt));
            session.complete(cs);
            if (action == NativeTradeAction.NAK_HAGGLE) {
                unit.setMovesLeft(0); // Clear moves again on hagglers
                cs.addPartial(See.only(otherPlayer), unit, "movesLeft");
            }                
            break;
            
        default:
            return serverPlayer.clientError("Bogus action: " + action);
        }

        // Update the other player if needed
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Pay arrears.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param type The {@code GoodsType} to pay the arrears for.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet payArrears(ServerPlayer serverPlayer, GoodsType type) {
        int arrears = serverPlayer.getArrears(type);
        if (arrears <= 0) {
            return serverPlayer.clientError("No arrears for pay for: "
                + type.getId());
        } else if (!serverPlayer.checkGold(arrears)) {
            return serverPlayer.clientError("Not enough gold to pay arrears for: "
                + type.getId());
        }

        ChangeSet cs = new ChangeSet();
        Market market = serverPlayer.getMarket();
        serverPlayer.modifyGold(-arrears);
        market.setArrears(type, 0);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), market.getMarketData(type));
        // Arrears payment is private.
        return cs;
    }


    /**
     * Pay for a building.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the
     *     colony.
     * @param colony The {@code Colony} that is building.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet payForBuilding(ServerPlayer serverPlayer, Colony colony) {
        if (!getGame().getSpecification()
            .getBoolean(GameOptions.PAY_FOR_BUILDING)) {
            return serverPlayer.clientError("Pay for building is disabled");
        }

        BuildableType build = colony.getCurrentlyBuilding();
        if (build == null) {
            return serverPlayer.clientError("Colony " + colony.getId()
                + " is not building anything!");
        }
        List<AbstractGoods> required = colony.getRequiredGoods(build);
        int price = colony.priceGoodsForBuilding(required);
        if (!serverPlayer.checkGold(price)) {
            return serverPlayer.clientError("Insufficient funds to pay for build.");
        }

        // Save the correct final gold for the player, as we are going to
        // use buy() below, but it deducts the normal uninflated price for
        // the goods being bought.  We restore this correct amount later.
        int savedGold = serverPlayer.modifyGold(-price);
        serverPlayer.modifyGold(price);

        ChangeSet cs = new ChangeSet();
        GoodsContainer container = colony.getGoodsContainer();
        container.saveState();
        for (AbstractGoods ag : required) {
            GoodsType type = ag.getType();
            int amount = ag.getAmount();
            if (type.isStorable()) {
                // FIXME: should also check canTrade(type, Access.?)
                if ((amount = serverPlayer.buy(container, type, amount)) < 0) {
                    return serverPlayer.clientError("Can not buy " + amount
                        + " " + type + " for " + build);
                }
                serverPlayer.propagateToEuropeanMarkets(type, -amount, random);
                serverPlayer.csFlushMarket(type, cs);
            } else {
                container.addGoods(type, amount);
            }
        }
        colony.invalidateCache();

        // Nothing to see for others, colony internal.
        serverPlayer.setGold(savedGold);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), container);
        return cs;
    }


    /**
     * Put outside colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to be put out.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet putOutsideColony(ServerPlayer serverPlayer, Unit unit) {
        Tile tile = unit.getTile();
        Colony colony = unit.getColony();
        if (unit.isInColony()) tile.cacheUnseen();//+til
        unit.setLocation(tile);//-vis: safe/colony,-til if in colony

        // Full tile update for the player, the rest get their limited
        // view of the colony so that population changes.
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), tile);
        cs.add(See.perhaps().except(serverPlayer), colony);
        return cs;
    }


    /**
     * Rearrange a colony.
     *
     * @param serverPlayer The {@code ServerPlayer} that is querying.
     * @param colony The {@code Colony} to rearrange.
     * @param arrangements A list of {@code Arrangement}s to apply.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet rearrangeColony(ServerPlayer serverPlayer, Colony colony,
                                     List<Arrangement> arrangements) {
        final Role defaultRole = getGame().getSpecification().getDefaultRole();
        Tile tile = colony.getTile();
        tile.cacheUnseen();//+til

        // Move everyone out of the way and stockpile their equipment.
        for (Arrangement a : arrangements) {
            a.unit.setLocation(tile);//-til
            if (!a.unit.hasDefaultRole()) {
                colony.equipForRole(a.unit, defaultRole, 0);
            }
        }

        List<Arrangement> todo = new ArrayList<>(arrangements);
        while (!todo.isEmpty()) {
            Arrangement a = todo.remove(0);
            if (a.loc == tile) continue;
            WorkLocation wl = (WorkLocation)a.loc;
            // Adding to wl can fail, and in the worst case there
            // might be a circular dependency.  If the move can
            // succeed, do it, but if not, retry.
            switch (wl.getNoAddReason(a.unit)) {
            case NONE:
                a.unit.setLocation(wl);
                // Fall through
            case ALREADY_PRESENT:
                if (a.unit.getWorkType() != a.work) {
                    a.unit.changeWorkType(a.work);
                }
                break;
            case CAPACITY_EXCEEDED:
                todo.add(todo.size(), a);
                break;
            default:
                logger.warning("Bad move for " + a.unit + " to " + wl);
                break;
            }
        }

        // Collect roles that cause a change, ordered by simplest change
        for (Arrangement a : transform(arrangements,
                a -> a.role != a.unit.getRole() && a.role != defaultRole,
                Function.identity(),
                Comparator.<Arrangement>reverseOrder())) {
            if (!colony.equipForRole(a.unit, a.role, a.roleCount)) {
                return serverPlayer.clientError("Failed to equip "
                    + a.unit.getId() + " for role " + a.role
                    + " at " + colony);
            }
        }
        
        // Just update the whole tile, including for other players
        // which might see colony population change.
        return new ChangeSet().add(See.perhaps(), tile);
    }


    /**
     * Rename an object.
     *
     * @param serverPlayer The {@code ServerPlayer} that is naming.
     * @param object The {@code Nameable} to rename.
     * @param newName The new name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet renameObject(ServerPlayer serverPlayer, Nameable object,
                                  String newName) {
        ChangeSet cs = new ChangeSet();

        if (object instanceof Settlement) {
            ((Settlement)object).getTile().cacheUnseen();//+til
        }
        object.setName(newName);//-til?
        FreeColGameObject fcgo = (FreeColGameObject)object;
        cs.addPartial(See.all(), fcgo, "name");

        // Others may be able to see the name change.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Handle a player retiring.
     *
     * @param serverPlayer The {@code ServerPlayer} that is retiring.
     * @return A {@code ChangeSet} containing the response.
     */
    public ChangeSet retire(ServerPlayer serverPlayer) {
        boolean highScore = HighScore.newHighScore(serverPlayer);
        ChangeSet cs = new ChangeSet();
        serverPlayer.csWithdraw(cs); // Clean up the player.
        getGame().sendToOthers(serverPlayer, cs);
        cs.addAttribute(See.only(serverPlayer),
                        "highScore", Boolean.toString(highScore));
        return cs;
    }


    /**
     * Scout a native settlement, that is, the contacting action
     * that generates the greeting dialog.
     *
     * @param serverPlayer The {@code ServerPlayer} that is scouting.
     * @param unit The scout {@code Unit}.
     * @param is The {@code IndianSettlement} to scout.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet scoutIndianSettlement(ServerPlayer serverPlayer,
                                           Unit unit, IndianSettlement is) {
        final Player owner = is.getOwner();
        ChangeSet cs = new ChangeSet();
        Tile tile = is.getTile();

        csVisit(serverPlayer, is, -1, cs);
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        cs.add(See.only(serverPlayer), ChangeSet.ChangePriority.CHANGE_LATE,
            new NationSummaryMessage(owner)
                .setNationSummary(new NationSummary(owner, serverPlayer)));

        // This is private.
        return cs;
    }


    /**
     * Speak to the chief at a native settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is scouting.
     * @param unit The scout {@code Unit}.
     * @param is The {@code IndianSettlement} to scout.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet scoutSpeakToChief(ServerPlayer serverPlayer,
                                       Unit unit, IndianSettlement is) {
        ChangeSet cs = new ChangeSet();
        Tile tile = is.getTile();
        boolean tileDirty = is.setVisited(serverPlayer);
        String result;

        // Hateful natives kill the scout right away.
        Tension tension = is.getAlarm(serverPlayer);
        if (tension.getLevel() == Tension.Level.HATEFUL) {
            cs.add(See.perhaps().always(serverPlayer),
                   (FreeColGameObject)unit.getLocation());
            cs.addRemove(See.perhaps().always(serverPlayer),
                         unit.getLocation(), unit);//-vis(serverPlayer)
            unit.dispose();
            serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
            result = "die";
        } else {
            // Otherwise player gets to visit, and learn about the settlement.
            List<UnitType> scoutTypes = getGame().getSpecification()
                .getUnitTypesWithAbility(Ability.EXPERT_SCOUT);
            UnitType scoutSkill = first(scoutTypes);
            int radius = unit.getLineOfSight();
            UnitType skill = is.getLearnableSkill();
            int rnd = randomInt(logger, "scouting", random, 10);
            if (is.hasAnyScouted()) {
                // Do nothing if already spoken to.
                result = "nothing";
            } else if (scoutSkill != null && unit.getType() != scoutSkill
                && ((skill != null && skill.hasAbility(Ability.EXPERT_SCOUT))
                    || rnd == 0)) {
                // If the scout can be taught to be an expert it will be.
                unit.changeType(scoutSkill);//-vis(serverPlayer)
                serverPlayer.invalidateCanSeeTiles();//+vis(serverPlayer)
                result = "expert";
            } else {
                // Choose tales 1/3 of the time, or if there are no beads.
                RandomRange gifts = is.getType().getGifts(unit);
                int gold = (gifts == null) ? 0
                    : gifts.getAmount("Base beads amount", random, true);
                if (gold <= 0 || rnd <= 3) {
                    radius = Math.max(radius, IndianSettlement.TALES_RADIUS);
                    result = "tales";
                } else {
                    if (unit.hasAbility(Ability.EXPERT_SCOUT)) {
                        gold = (gold * 11) / 10; // FIXME: magic number
                    }
                    serverPlayer.modifyGold(gold);
                    is.getOwner().modifyGold(-gold);
                    result = Integer.toString(gold);
                    cs.addPartial(See.only(serverPlayer), serverPlayer,
                                  "gold", "score");
                }
            }

            // Have now spoken to the chief.
            csVisit(serverPlayer, is, 1, cs);
            tileDirty = true;

            // Update settlement tile with new information, and any
            // newly visible tiles, possibly with enhanced radius.
            Set<Tile> tiles = transform(tile.getSurroundingTiles(1, radius),
                t -> !serverPlayer.canSee(t) && (t.isLand() || t.isShore()),
                Function.identity(), Collectors.toSet());
            cs.add(See.only(serverPlayer), serverPlayer.exploreTiles(tiles));

            // If the unit was promoted, update it completely, otherwise just
            // update moves and possibly gold+score.
            unit.setMovesLeft(0);
            if ("expert".equals(result)) {
                cs.add(See.perhaps(), unit);
            } else {
                cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
            }
        }
        if (tileDirty) {
            tile.updateIndianSettlement(serverPlayer);
            cs.add(See.only(serverPlayer), tile);
        }

        // Always add result.
        cs.add(See.only(serverPlayer), ChangePriority.CHANGE_LATE,
            new ScoutSpeakToChiefMessage(unit, is, result));

        // Other players may be able to see unit disappearing, or
        // learning.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Sell goods in Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} that is selling.
     * @param type The {@code GoodsType} to sell.
     * @param amount The amount of goods to sell.
     * @param carrier The {@code Unit} carrying the goods.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet sellGoods(ServerPlayer serverPlayer, GoodsType type,
                               int amount, Unit carrier) {
        ChangeSet cs = new ChangeSet();
        GoodsContainer container = carrier.getGoodsContainer();
        container.saveState();
        if (serverPlayer.canTrade(type, Access.EUROPE)) {
            int gold = serverPlayer.getGold();
            int sellAmount = serverPlayer.sell(container, type, amount);
            if (sellAmount < 0) {
                return serverPlayer.clientError("Player " + serverPlayer.getName()
                    + " tried to sell " + amount + " " + type.getSuffix());
            }
            serverPlayer.propagateToEuropeanMarkets(type, sellAmount, random);
            serverPlayer.csFlushMarket(type, cs);
            cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
            logger.finest(carrier + " sold " + amount + "(" + sellAmount + ")"
                + " " + type.getSuffix()
                + " in Europe for " + (serverPlayer.getGold() - gold));
        } else {
            // Dumping goods in Europe
            moveGoods(carrier, type, amount, null);
            logger.finest(carrier + " dumped " + amount
                + " " + type.getSuffix() + " in Europe");
        }
        carrier.setMovesLeft(0);
        cs.add(See.only(serverPlayer), carrier);
        // Action occurs in Europe, nothing is visible to other players.
        return cs;
    }


    /**
     * Price some goods for sale to a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is buying.
     * @param unit The {@code Unit} that is trading.
     * @param settlement The {@code Settlement} that is trading.
     * @param goods The {@code Goods} to sell.
     * @param price The sellers proposed price for the goods.
     * @return A {@code SellPropositionMessage} encapsulating this action
     *     or an error message on failure.
     */
    public DOMMessage sellProposition(ServerPlayer serverPlayer,
        Unit unit, Settlement settlement, Goods goods, int price) {
        NativeTradeSession session
            = Session.lookup(NativeTradeSession.class, unit, settlement);
        if (session == null) {
            return new ErrorMessage("Proposing to sell without opening a session");
        }
        NativeTrade nt = session.getNativeTrade();
        if (!nt.getSell()) {
            return new ErrorMessage("Proposing to sell in a session where selling is not allowed.");
        }
        ChangeSet cs = new ChangeSet();
        if (settlement instanceof IndianSettlement) {
            csVisit(serverPlayer, (IndianSettlement)settlement, 0, cs);
        }

        // AI considers the proposition, return with a gold value
        AIPlayer ai = getFreeColServer().getAIPlayer(settlement.getOwner());
        int gold = ai.sellProposition(unit, settlement, goods, price);

        // Others can not see proposals.
        return new SellPropositionMessage(unit, settlement, goods, gold);
    }


    /**
     * Sell to a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is selling.
     * @param unit The {@code Unit} carrying the goods.
     * @param is The {@code IndianSettlement} to sell to.
     * @param goods The {@code Goods} to sell.
     * @param amount How much gold to expect.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet sellToSettlement(ServerPlayer serverPlayer, Unit unit,
                                      IndianSettlement is,
                                      Goods goods, int amount) {
        ChangeSet cs = new ChangeSet();
        csVisit(serverPlayer, is, 0, cs);

        NativeTradeSession session
            = Session.lookup(NativeTradeSession.class, unit, is);
        if (session == null) {
            return serverPlayer.clientError("Trying to sell without opening a session");
        }
        NativeTrade nt = session.getNativeTrade();
        if (!nt.getSell()) {
            return serverPlayer.clientError("Trying to sell in a session where selling is not allowed.");
        }
        final Player settlementPlayer = is.getOwner();

        // Check that the gold is the agreed amount
        AIPlayer ai = getFreeColServer().getAIPlayer(settlementPlayer);
        int returnGold = ai.sellProposition(unit, is, goods, amount);
        if (returnGold != amount) {
            return serverPlayer.clientError("This was not the price we agreed upon! Cheater?");
        }

        // Valid, make the trade.
        moveGoods(unit, goods.getType(), goods.getAmount(), is);
        cs.add(See.perhaps(), unit);

        settlementPlayer.modifyGold(-amount);
        serverPlayer.modifyGold(amount);
        ((ServerIndianSettlement)is).csModifyAlarm(serverPlayer,
            -amount / 500, true, cs);
        final Tile tile = is.getTile();
        is.updateWantedGoods();
        tile.updateIndianSettlement(serverPlayer);
        cs.add(See.only(serverPlayer), tile);
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        nt.setSell(true);
        cs.addSale(serverPlayer, is, goods.getType(),
                Math.round((float) amount / goods.getAmount()));
        logger.finest(serverPlayer.getName() + " " + unit + " sells " + goods
                      + " at " + is.getName() + " for " + amount);

        // Others can see the unit capacity.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Set build queue.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the colony.
     * @param colony The {@code Colony} to set the queue of.
     * @param queue The new build queue.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setBuildQueue(ServerPlayer serverPlayer, Colony colony,
                                   List<BuildableType> queue) {
        BuildableType current = colony.getCurrentlyBuilding();
        colony.setBuildQueue(queue);
        if (getGame().getSpecification()
            .getBoolean(GameOptions.CLEAR_HAMMERS_ON_CONSTRUCTION_SWITCH)
            && current != colony.getCurrentlyBuilding()) {
            for (AbstractGoods ag : transform(current.getRequiredGoods(),
                    g -> !g.getType().isStorable())) {
                colony.removeGoods(ag.getType());
            }
        }
        colony.invalidateCache();

        // Only visible to player.
        ChangeSet cs = new ChangeSet();
        cs.add(See.only(serverPlayer), colony);
        return cs;
    }


    /**
     * Set a unit stop.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to set the destination for.
     * @param index The stop index.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setCurrentStop(ServerPlayer serverPlayer, Unit unit,
                                    int index) {
        TradeRoute tr = unit.getTradeRoute();
        if (tr == null) {
            return serverPlayer.clientError("Unit has no trade route to set stop for.");
        } else if (index < 0 || index >= tr.getStops().size()) {
            return serverPlayer.clientError("Stop index out of range [0.."
                + tr.getStops().size() + "]: " + index);
        }

        unit.setCurrentStop(index);

        // Others can not see a stop change.
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Set a unit destination.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to set the destination for.
     * @param destination The {@code Location} to set as destination.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setDestination(ServerPlayer serverPlayer, Unit unit,
                                    Location destination) {
        if (unit.getTradeRoute() != null) {
            // Override destination to bring the unit to port.
            if (destination == null && unit.isAtSea()) {
                destination = unit.getStop().getLocation();
            }
            unit.setTradeRoute(null);
        }
        unit.setDestination(destination);

        // Others can not see a destination change.
        return new ChangeSet().add(See.only(serverPlayer), unit);
    }


    /**
     * Set goods levels.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the colony.
     * @param colony The {@code Colony} to set the goods levels in.
     * @param exportData The new {@code ExportData}.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setGoodsLevels(ServerPlayer serverPlayer, Colony colony,
                                    ExportData exportData) {
        colony.setExportData(exportData);
        return new ChangeSet().add(See.only(serverPlayer), colony);
    }


    /**
     * Set land name.
     *
     * @param serverPlayer The {@code ServerPlayer} who landed.
     * @param unit The {@code Unit} that has come ashore.
     * @param name The new land name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setNewLandName(ServerPlayer serverPlayer, Unit unit,
                                    String name) {
        ChangeSet cs = new ChangeSet();

        // Special case of a welcome from an adjacent native unit,
        // offering the land the landing unit is on if a peace treaty
        // is accepted.
        serverPlayer.setNewLandName(name);

        // Update the name and note the history.
        cs.addPartial(See.only(serverPlayer), serverPlayer, "newLandName");
        Turn turn = serverPlayer.getGame().getTurn();
        HistoryEvent h = new HistoryEvent(turn,
            HistoryEvent.HistoryEventType.DISCOVER_NEW_WORLD, serverPlayer)
                .addName("%name%", name);
        cs.addHistory(serverPlayer, h);
        return cs;
    }


    /**
     * Set region name.
     *
     * @param serverPlayer The {@code ServerPlayer} discovering.
     * @param unit The {@code Unit} that is discovering.
     * @param region The {@code Region} to discover.
     * @param name The new region name.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet setNewRegionName(ServerPlayer serverPlayer, Unit unit,
                                      Region region, String name) {
        final Game game = getGame();
        ServerRegion serverRegion = (ServerRegion)region;
        // Discoverer is set when unit moves in.
        if (!Utils.equals(region.getDiscoverer(), unit.getId())) {
            return serverPlayer.clientError("Discoverer mismatch, "
                + region.getDiscoverer() + " expected, "
                + unit.getId() + " provided.");
        }
        ChangeSet cs = new ChangeSet();
        serverRegion.csDiscover(serverPlayer, game.getTurn(), name, cs);

        // Others do find out about region name changes.
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }


    /**
     * Spy on a settlement.
     *
     * @param serverPlayer The {@code ServerPlayer} that is spying.
     * @param unit The {@code Unit} that is spying.
     * @param settlement The {@code Settlement} to spy on.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet spySettlement(ServerPlayer serverPlayer, Unit unit,
                                   Settlement settlement) {
        ChangeSet cs = new ChangeSet();

        cs.addSpy(See.only(serverPlayer), unit, settlement);
        unit.setMovesLeft(0);
        cs.addPartial(See.only(serverPlayer), unit, "movesLeft");
        return cs;
    }


    /**
     * Train a unit in Europe.
     *
     * @param serverPlayer The {@code ServerPlayer} that is demanding.
     * @param type The {@code UnitType} to train.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet trainUnitInEurope(ServerPlayer serverPlayer,
                                       UnitType type) {

        Europe europe = serverPlayer.getEurope();
        if (europe == null) {
            return serverPlayer.clientError("No Europe to train in.");
        }
        int price = europe.getUnitPrice(type);
        if (price <= 0) {
            return serverPlayer.clientError("Bogus price: " + price);
        } else if (!serverPlayer.checkGold(price)) {
            return serverPlayer.clientError("Not enough gold ("
                + serverPlayer.getGold() + " < " + price
                + ") to train " + type);
        }

        final Game game = getGame();
        final Specification spec = game.getSpecification();
        Role role = (spec.getBoolean(GameOptions.EQUIP_EUROPEAN_RECRUITS))
            ? type.getDefaultRole()
            : spec.getDefaultRole();
        Unit unit = new ServerUnit(game, europe, serverPlayer, type,
                                   role);//-vis: safe, Europe
        unit.setName(serverPlayer.getNameForUnit(type, random));
        serverPlayer.modifyGold(-price);
        ((ServerEurope)europe).increasePrice(type, price);

        // Only visible in Europe
        ChangeSet cs = new ChangeSet();
        cs.addPartial(See.only(serverPlayer), serverPlayer, "gold");
        cs.add(See.only(serverPlayer), europe);
        return cs;
    }


    /**
     * Unload goods.
     *
     * @param serverPlayer The {@code ServerPlayer} that is unloading.
     * @param goodsType The {@code GoodsType} to unload.
     * @param amount The amount of goods to unload.
     * @param carrier The {@code Unit} to unload.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet unloadGoods(ServerPlayer serverPlayer, GoodsType goodsType,
                                 int amount, Unit carrier) {
        if (carrier.getGoodsCount(goodsType) < amount) {
            return serverPlayer.clientError("Too few goods");
        }
        if (carrier.isInEurope()) {
            return sellGoods(serverPlayer, goodsType, amount, carrier);
        }

        ChangeSet cs = new ChangeSet();
        if (carrier.getSettlement() != null) {
            Settlement settlement = carrier.getSettlement();
            moveGoods(carrier, goodsType, amount, settlement);
            logger.finest(carrier
                + " unloaded " + amount + " " + goodsType.getSuffix()
                + " to " + settlement.getName());
            cs.add(See.only(serverPlayer), settlement.getGoodsContainer());
            cs.add(See.only(serverPlayer), carrier.getGoodsContainer());
            if (carrier.getInitialMovesLeft() != carrier.getMovesLeft()) {
                carrier.setMovesLeft(0);
                cs.addPartial(See.only(serverPlayer), carrier, "movesLeft");
            }
        } else { // Dump of goods onto a tile
            moveGoods(carrier, goodsType, amount, null);
            logger.finest(carrier + " dumped " + amount
                + " " + goodsType.getSuffix() + " to " + carrier.getLocation());
            cs.add(See.perhaps(), (FreeColGameObject)carrier.getLocation());
            // Others might see a capacity change.
            getGame().sendToOthers(serverPlayer, cs);
        }
        return cs;
    }


    /**
     * Update a trade route for a player.
     *
     * @param serverPlayer The {@code ServerPlayer} to set trade
     *     routes for.
     * @param tradeRoute An uninterned {@code TradeRoute} to update.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet updateTradeRoute(ServerPlayer serverPlayer,
                                      TradeRoute tradeRoute) {
        final Game game = getGame();
        String name;
        StringTemplate fail;
        TradeRoute tr;
        if (tradeRoute == null || tradeRoute.getId() == null
            || (name = tradeRoute.getName()) == null) {
            return serverPlayer.clientError("Bogus route");
        } else if ((fail = tradeRoute.verify()) != null) {
            return serverPlayer.clientError(Messages.message(fail));
        } else if ((tr = game.getFreeColGameObject(tradeRoute.getId(),
                    TradeRoute.class)) == null) {
            return serverPlayer.clientError("Not an existing trade route: "
                + tradeRoute.getId());
        }
        tr.updateFrom(tradeRoute);

        // Have to update the whole player alas.
        return new ChangeSet().add(See.only(serverPlayer), serverPlayer);
    }


    /**
     * Change work location.
     *
     * @param serverPlayer The {@code ServerPlayer} that owns the unit.
     * @param unit The {@code Unit} to change the work location of.
     * @param workLocation The {@code WorkLocation} to change to.
     * @return A {@code ChangeSet} encapsulating this action.
     */
    public ChangeSet work(ServerPlayer serverPlayer, Unit unit,
                          WorkLocation workLocation) {
        final Specification spec = getGame().getSpecification();
        final Colony colony = workLocation.getColony();
        colony.getGoodsContainer().saveState();

        ChangeSet cs = new ChangeSet();
        Tile tile = workLocation.getWorkTile();
        if (tile != null && tile.getOwningSettlement() != colony) {
            // Claim known free land (because canAdd() succeeded).
            serverPlayer.csClaimLand(tile, colony, 0, cs);
        }

        colony.equipForRole(unit, spec.getDefaultRole(), 0);

        // Check for upgrade.
        UnitChange uc = unit.getUnitChange(UnitChangeType.ENTER_COLONY);
        if (uc != null) unit.changeType(uc.to);//-vis: safe in colony

        // Change the location.
        // We could avoid updating the whole tile if we knew that this
        // was definitely a move between locations and no student/teacher
        // interaction occurred.
        if (!unit.isInColony()) unit.getColony().getTile().cacheUnseen();//+til
        unit.setLocation(workLocation);//-vis: safe/colony,-til if not in colony
        cs.add(See.perhaps(), colony.getTile());
        // Others can see colony change size
        getGame().sendToOthers(serverPlayer, cs);
        return cs;
    }
}
