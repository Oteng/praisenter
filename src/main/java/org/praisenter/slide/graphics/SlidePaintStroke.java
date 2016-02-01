package org.praisenter.slide.graphics;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.praisenter.slide.SlidePaintXmlAdapter;
import org.praisenter.utility.Maf;

@XmlRootElement(name = "paintStroke")
@XmlAccessorType(XmlAccessType.NONE)
public final class SlidePaintStroke extends AbstractSlideStroke implements SlideStroke {
	@XmlElement(name = "paint", required = false)
//	@XmlElementRefs({
//		@XmlElementRef(name = "paint", type = SlideColor.class),
//		@XmlElementRef(name = "paint", type = SlideLinearGradient.class),
//		@XmlElementRef(name = "paint", type = SlideRadialGradient.class)
//	})
	@XmlJavaTypeAdapter(value = SlidePaintXmlAdapter.class)
	final SlidePaint paint;
	
	@XmlElement(name = "style", required = false)
	final SlideStrokeStyle style;
	
	@XmlAttribute(name = "width", required = false)
	final double width;
	
	@XmlAttribute(name = "radius", required = false)
	final double radius;
	
	private SlidePaintStroke() {
		// for jaxb
		this.paint = null;
		this.style = null;
		this.width = 0;
		this.radius = 0;
	}
	
	public SlidePaintStroke(SlidePaint paint, SlideStrokeStyle style, double width, double radius) {
		this.paint = paint;
		this.style = style;
		this.width = width;
		this.radius = radius;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj == this) return true;
		if (obj instanceof SlidePaintStroke) {
			SlidePaintStroke s = (SlidePaintStroke)obj;
			if (!this.paint.equals(s.paint) ||
				!this.style.equals(s.style) ||
				!Maf.equals(this.radius, s.radius) ||
				!Maf.equals(this.width, s.width)) {
				return false;
			}
			return true;
		}
		return false;
	}

	public SlidePaint getPaint() {
		return paint;
	}

	public SlideStrokeStyle getStyle() {
		return style;
	}

	public double getWidth() {
		return width;
	}

	public double getRadius() {
		return radius;
	}
}
