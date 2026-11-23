import metajavbots.api.*;

/**
 * My first metajavbot. Change whatever you like and press Compile (Ctrl+S).
 * The «on…» methods are called by themselves when something happens; onTick about 30 times a second.
 */
public class MyRobot extends MetajavbotEn {

    @Override
    public void onStart() {
        say("Let's fight!");
    }

    @Override
    public void onTick() {
        // When it got where it was going, move on a bit and turn.
        if (remainingDistance() == 0) {
            ahead(120);
            turn(35);
        }
        // Move the sight (arms and radar) to look for the enemy.
        // Careful: onTick runs after onSeeEnemy and the sight only remembers the last order: this one erases that one.
        aim(12);
    }

    @Override
    public void onSeeEnemy(Enemy e) {
        // The sight turns slowly (about 14° per tick): it does not reach the enemy in this same tick.
        aimTo(e.angle());
        // attack() fires right away, where the sight points NOW, not where you just asked it to go.
        attack();
    }

    @Override
    public void onHit(Hit h) {
        // Dodge to the side.
        turnTo(h.angle() + 90);
        ahead(80);
    }
}
