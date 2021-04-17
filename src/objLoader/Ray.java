package objLoader;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Ray {

	private Vertex origin, distVertex;
	private Vector direction;
	private double renderDist;
	
	Ray(Vertex origin, Vector direction, double renderDist) {
		this.origin = origin;
		this.direction = direction;
		this.renderDist = renderDist;
		this.distVertex = new Vertex(
				origin.getX()+renderDist*direction.getX(),
				origin.getY()+renderDist*direction.getY(),
				origin.getZ()+renderDist*direction.getZ());
	}
	
	public Vertex march(ArrayList<OBJ> objs) {
		double dist = getClosestFace(origin, objs);
		Vertex start = new Vertex(
				origin.getX()+dist*direction.getX(),
				origin.getY()+dist*direction.getY(),
				origin.getZ()+dist*direction.getZ());
		double marchDist = get3DDist(origin, start);
		while (dist > .3 && marchDist < renderDist) {
			dist = getClosestFace(start, objs);
			start = new Vertex(
					start.getX()+dist*direction.getX(),
					start.getY()+dist*direction.getY(),
					start.getZ()+dist*direction.getZ());
			marchDist = get3DDist(origin, start);
		}
		return start;
	}
	
	public Color getDistColor(Vertex distVertex) {
		double dist = get3DDist(origin, distVertex);
		int shade = renderDist/dist > 255 ? 255 : (int)(renderDist/dist);
		return new Color(shade, shade, shade, 255);
	}
	
	public double getClosestFace(Vertex start, ArrayList<OBJ> objs) {
		double dist = 1000000;
		for (OBJ obj : objs) {
			for (Face face : obj.getFaces()) {
				Vector faceNormal = face.getNormal().getDirection();
				double faceNormalX = face.getNormal().getDirection().getX();
				double faceNormalY = face.getNormal().getDirection().getY();
				double faceNormalZ = face.getNormal().getDirection().getZ();
				double t = 0;
				t = (face.getConstant() - 
						(faceNormalX * start.getX() + 
						faceNormalY * start.getY() + 
						faceNormalZ * start.getZ())) /
							(faceNormalX * faceNormalX +
							faceNormalY * faceNormalY +
							faceNormalZ * faceNormalZ);
				Vertex point = new Vertex(start.getX() + faceNormalX * t, 
						start.getY() + faceNormalY * t, 
						start.getZ() + faceNormalZ * t);
				
				Vertex planeOrigin = face.getVerts()[0];
				Vector xAxis = new Vector(
						planeOrigin.getX() + face.getVerts()[1].getX(), 
						planeOrigin.getY() + face.getVerts()[1].getY(), 
						planeOrigin.getZ() + face.getVerts()[1].getZ());
				Vector yAxis = faceNormal.CrossProduct(xAxis);
				
				Point2D.Double[] coordinates = new Point2D.Double[4];
				for (int i = 0; i < 4; i++) {
					if (i < 3) {
						coordinates[i] = new Point2D.Double(
							xAxis.dotProduct(
								new Vector(
									face.getVerts()[i].getX()-planeOrigin.getX(), 
									face.getVerts()[i].getY()-planeOrigin.getY(), 
									face.getVerts()[i].getZ()-planeOrigin.getZ())),
							yAxis.dotProduct(
								new Vector(
									face.getVerts()[i].getX()-planeOrigin.getX(), 
									face.getVerts()[i].getY()-planeOrigin.getY(), 
									face.getVerts()[i].getZ()-planeOrigin.getZ())));
					} else {
						coordinates[i] = new Point2D.Double(
							xAxis.dotProduct(
								new Vector(
									point.getX()-planeOrigin.getX(), 
									point.getY()-planeOrigin.getY(), 
									point.getZ()-planeOrigin.getZ())),
							yAxis.dotProduct(
									new Vector(
										point.getX()-planeOrigin.getX(), 
										point.getY()-planeOrigin.getY(), 
										point.getZ()-planeOrigin.getZ())));
					}
				}
				if (pointInside(coordinates)) {
					double newDist = Math.sqrt(
							Math.pow(start.getX() - point.getX(), 2) +	
							Math.pow(start.getY() - point.getY(), 2) + 
							Math.pow(start.getZ() - point.getZ(), 2));
					if (newDist < dist) {
						dist = newDist;
					}
				} else {
					double minDist = 
							get2DDist(coordinates[0], coordinates[3]) + 
							get2DDist(coordinates[1], coordinates[3]);
					int id = 0;
					for (int i = 1; i < 3; i++) {
						double newDist = 
								get2DDist(coordinates[i%3], coordinates[3]) + 
								get2DDist(coordinates[(i+1)%3], coordinates[3]);
						if (newDist < minDist) {
							minDist = newDist;
							id = i;
						}
					}
					Vector2D edge = new Vector2D(
							coordinates[id].getX()-coordinates[(id+1)%3].getX(),
							coordinates[id].getY()-coordinates[(id+1)%3].getY());
					double edgeB = -(edge.getY()/edge.getX())*coordinates[id].getX() + coordinates[id].getY();
					Vector2D intersect = edge.getPerpendicularVector();
					double intersectB = -(intersect.getY()/intersect.getX())*coordinates[3].getX() + coordinates[3].getY();
					double x = (edgeB-intersectB)/((intersect.getY()/intersect.getX())-(edge.getY()/edge.getX()));
					Point2D.Double newPoint2D = new Point2D.Double(x, (edge.getY()/edge.getX())*x + edgeB);
					
					point = new Vertex(
							planeOrigin.getX()+(newPoint2D.getX()*xAxis.getX())+(newPoint2D.getY()*yAxis.getX()), 
							planeOrigin.getY()+(newPoint2D.getX()*xAxis.getY())+(newPoint2D.getY()*yAxis.getY()),
							planeOrigin.getZ()+(newPoint2D.getX()*xAxis.getZ())+(newPoint2D.getY()*yAxis.getZ()));
					
					double diffX = point.getX() - start.getX();
					double diffY = point.getY() - start.getY();
					double diffZ = point.getZ() - start.getZ();
					double newDist = Math.sqrt(
							diffX * diffX +	
							diffY * diffY + 
							diffZ * diffZ);
					if (newDist < dist) {
						dist = newDist;
					}
				}
			}
		}
		return dist;
	}

	public double get2DDist(Point2D.Double point1, Point2D.Double point2) {
		return Math.sqrt(Math.pow(point1.getX() - point2.getX(), 2) +	Math.pow(point1.getY() - point2.getY(), 2));
	}
	
	public double get3DDist(Vertex point1, Vertex point2) {
		return Math.sqrt(Math.pow(point1.getX() - point2.getX(), 2) +	Math.pow(point1.getY() - point2.getY(), 2) + Math.pow(point1.getZ() - point2.getZ(), 2));
	}
	
	public boolean pointInside(Point2D.Double[] coordinates) {
		double area = triangleAreaFromPoints(coordinates[0], coordinates[1], coordinates[2]);
		double totalArea = 0;
		for (int i = 0; i < 3; i++) {
			totalArea += triangleAreaFromPoints(coordinates[i%3], coordinates[(i+1)%3], coordinates[3]);
		}
		return totalArea <= area;
	}
	
	public static double triangleAreaFromPoints(Point2D.Double point1, Point2D.Double point2, Point2D.Double point3) {
		return Math.abs((point3.getX()*(point1.getY()-point2.getY()) + point1.getX()*(point2.getY()-point3.getY()) + point2.getX()*(point3.getY()-point1.getY()))/2);
	}
	
	public Vertex getOrigin() {
		return origin;
	}

	public Vector getDirection() {
		return direction;
	}

	public Vertex getDistVertex() {
		return distVertex;
	}
}
