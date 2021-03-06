package fr.neatmonster.nocheatplus.checks.moving;

import java.util.Locale;

import org.bukkit.Bukkit;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.actions.types.ActionList;
import fr.neatmonster.nocheatplus.checks.CheckUtils;
import fr.neatmonster.nocheatplus.players.NCPPlayer;
import fr.neatmonster.nocheatplus.players.informations.Statistics.Id;

/**
 * A check to see if people cheat by tricking the server to not deal them
 * fall damage.
 * 
 */
public class NoFallCheck extends MovingCheck {

    public class NoFallCheckEvent extends MovingEvent {

        public NoFallCheckEvent(final NoFallCheck check, final NCPPlayer player, final ActionList actions,
                final double vL) {
            super(check, player, actions, vL);
        }
    }

    public NoFallCheck() {
        super("nofall");
    }

    /**
     * Calculate if and how much the player "failed" this check.
     * 
     */
    public void check(final NCPPlayer player, final Object... args) {
        final MovingConfig cc = getConfig(player);
        final MovingData data = getData(player);

        // If the player is serverside in creative mode, we have to stop here to
        // avoid hurting him when he switches back to "normal" mode
        if (player.canFly()) {
            data.fallDistance = 0F;
            data.lastAddedFallDistance = 0F;
            return;
        }

        // If the player is in ladder or unclimbable vines, do not do the check
        if (CheckUtils.isLadder(CheckUtils.evaluateLocation(player.getWorld(), data.from))
                || CheckUtils.isLadder(CheckUtils.evaluateLocation(player.getWorld(), data.to))
                || CheckUtils.isVine(CheckUtils.evaluateLocation(player.getWorld(), data.from))
                || CheckUtils.isVine(CheckUtils.evaluateLocation(player.getWorld(), data.to))) {
            data.fallDistance = 0F;
            data.lastAddedFallDistance = 0F;
            return;
        }

        // This check is pretty much always a step behind for technical reasons.
        if (data.fromOnOrInGround)
            // Start with zero fall distance
            data.fallDistance = 0F;

        if (cc.nofallAggressive && data.fromOnOrInGround && data.toOnOrInGround && data.from.y <= data.to.y
                && player.getBukkitPlayer().getFallDistance() > 3.0F) {
            data.fallDistance = player.getBukkitPlayer().getFallDistance();
            data.nofallVL += data.fallDistance;
            incrementStatistics(player, Id.MOV_NOFALL, data.fallDistance);

            // Execute whatever actions are associated with this check and the
            // violation level and find out if we should cancel the event
            final boolean cancel = executeActions(player, cc.nofallActions, data.nofallVL);
            if (cancel)
                player.dealFallDamage();
            data.fallDistance = 0F;
        }

        // If we increased fall height before for no good reason, reduce now by
        // the same amount
        if (player.getBukkitPlayer().getFallDistance() > data.lastAddedFallDistance)
            player.getBukkitPlayer().setFallDistance(
                    player.getBukkitPlayer().getFallDistance() - data.lastAddedFallDistance);

        data.lastAddedFallDistance = 0;

        // We want to know if the fallDistance recorded by the game is smaller
        // than the fall distance recorded by the plugin
        final float difference = data.fallDistance - player.getBukkitPlayer().getFallDistance();

        if (difference > 1.0F && data.toOnOrInGround && data.fallDistance > 2.0F) {
            data.nofallVL += difference;
            incrementStatistics(player, Id.MOV_NOFALL, difference);

            // Execute whatever actions are associated with this check and the
            // violation level and find out if we should cancel the event
            final boolean cancel = executeActions(player, cc.nofallActions, data.nofallVL);

            // If "cancelled", the fall damage gets dealt in a way that's
            // visible to other plugins
            if (cancel) {
                // Increase the fall distance a bit :)
                final float totalDistance = data.fallDistance + difference * (cc.nofallMultiplier - 1.0F);

                player.getBukkitPlayer().setFallDistance(totalDistance);
            }

            data.fallDistance = 0F;
        }

        // Increase the fall distance that is recorded by the plugin, AND set
        // the fall distance of the player
        // to whatever he would get with this move event. This modifies
        // Minecrafts fall damage calculation
        // slightly, but that's still better than ignoring players that try to
        // use "teleports" or "stepdown"
        // to avoid falldamage. It is only added for big height differences
        // anyway, as to avoid to much deviation
        // from the original Minecraft feeling.

        final double oldY = data.from.y;
        final double newY = data.to.y;

        if (oldY > newY) {
            final float dist = (float) (oldY - newY);
            data.fallDistance += dist;

            if (dist > 1.0F) {
                data.lastAddedFallDistance = dist;
                player.getBukkitPlayer().setFallDistance(player.getBukkitPlayer().getFallDistance() + dist);
            } else
                data.lastAddedFallDistance = 0.0F;
        } else
            data.lastAddedFallDistance = 0.0F;

        // Reduce falldamage violation level
        data.nofallVL *= 0.95D;

        return;
    }

    @Override
    protected boolean executeActions(final NCPPlayer player, final ActionList actionList, final double violationLevel) {
        final NoFallCheckEvent event = new NoFallCheckEvent(this, player, actionList, violationLevel);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled())
            return super.executeActions(player, event.getActions(), event.getVL());
        return false;
    }

    @Override
    public String getParameter(final ParameterName wildcard, final NCPPlayer player) {

        if (wildcard == ParameterName.VIOLATIONS)
            return String.valueOf(Math.round(getData(player).nofallVL));
        else if (wildcard == ParameterName.FALLDISTANCE)
            return String.format(Locale.US, "%.2f", getData(player).fallDistance);
        else
            return super.getParameter(wildcard, player);
    }
}
