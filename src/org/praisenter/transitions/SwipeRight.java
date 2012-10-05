package org.praisenter.transitions;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;

import org.praisenter.resources.Messages;

/**
 * Represents a swipe right {@link Transition}.
 * @author William Bittle
 * @version 1.0.0
 * @since 1.0.0
 */
public class SwipeRight extends Transition {
	/** The {@link SwipeRight} transition id */
	public static final int ID = 30;
	
	/**
	 * Full constructor.
	 * @param type the transition type
	 */
	public SwipeRight(Type type) {
		super(Messages.getString("transition.swipeRight"), type);
	} 

	/* (non-Javadoc)
	 * @see org.praisenter.transitions.Transition#getTransitionId()
	 */
	@Override
	public int getTransitionId() {
		return ID;
	}
	
	/* (non-Javadoc)
	 * @see org.praisenter.transitions.Transition#render(java.awt.Graphics2D, java.awt.image.BufferedImage, java.awt.image.BufferedImage, double)
	 */
	@Override
	public void render(Graphics2D g2d, BufferedImage image0, BufferedImage image1, double pc) {
		Shape shape = g2d.getClip();
		if (image0 != null) {
			g2d.setClip((int)Math.ceil(image0.getWidth() * pc), 0, image0.getWidth(), image0.getHeight());
			g2d.drawImage(image0, 0, 0, null);
		}
		if (this.type == Transition.Type.IN && image1 != null) {
			g2d.setClip(0, 0, (int)Math.ceil(image1.getWidth() * pc), image1.getHeight());
			g2d.drawImage(image1, 0, 0, null);
		}
		g2d.setClip(shape);
	}
}
