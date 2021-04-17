package objLoader;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

public class Camera {

	private double renderWidth, renderHeight;
	private double[] pos, viewTo;
	private double zoom;
	private double fov;
	private ArrayList<DPolygon> polygonList;
	private int[] renderOrder;
	private double renderDist;

	// VertLook goes from 0.999 to -0.999, minus being looking down and + looking
	// up, HorLook takes any number and goes round in radians
	// aimSight changes the size of the center-cross. The lower HorRotSpeed or
	// VertRotSpeed, the faster the camera will rotate in those directions
	private double VertLook, HorLook, HorRotSpeed = 900, VertRotSpeed = 2200;

	double t = 0, aimSight = 4;
	private Vector W1, W2, ViewVector, RotationVector, DirectionVector, PlaneVector1, PlaneVector2;
	private Plane P;
	double[] CalcFocusPos = new double[2];

	public Camera(double renderWidth, double renderHeight, double[] pos, double vertLook, double horLook, double zoom, double fov) {
		this.renderWidth = renderWidth;
		this.renderHeight = renderHeight;
		this.pos = pos;
		this.VertLook = vertLook;
		this.HorLook = horLook;
		this.zoom = zoom;
		this.viewTo = new double[3];
		this.polygonList = new ArrayList<DPolygon>();
		this.fov = fov;
		this.renderDist = 50.0;
	}

	public BufferedImage renderOthrographicRays(ArrayList<OBJ> objs) {
		int[] pixels = new int[(int)renderWidth * (int)renderHeight*4];
		
		long startTime = System.currentTimeMillis();
		long lastTime;
		Vertex posVertex = new Vertex(pos[0], pos[1], pos[2]);
		
		int size = 13;
		double step = size/renderWidth;
		for (int y = 0; y < renderHeight; y++) {
			for (int x = 0; x < renderWidth; x++) {
				Ray ray = new Ray(new Vertex(posVertex.getX() - size/2 + step*x, 2.25, posVertex.getZ() + size/2 - step*y), new Vector(0, -1, 0), renderDist);
				Color c = ray.getDistColor(ray.march(objs));
				pixels[(y * (int)renderWidth + x) * 4] = c.getRed();
				pixels[(y * (int)renderWidth + x) * 4+1] = c.getGreen();
				pixels[(y * (int)renderWidth + x) * 4+2] = c.getBlue();
				pixels[(y * (int)renderWidth + x) * 4+3] = c.getAlpha();
				lastTime = System.currentTimeMillis();
				if (lastTime - startTime > 1000) {
					startTime = System.currentTimeMillis();
					System.out.printf("Percent Done: %.1f%%\n", 100*((double)y * renderWidth + (double)x)/
							(renderWidth*renderHeight));
				}
			}
		}
		
		return getImageFromArray(pixels, (int)renderWidth, (int)renderHeight);

	}
	
	public BufferedImage renderRayToScreen(ArrayList<OBJ> objs) {

		int[] pixels = new int[(int)renderWidth * (int)renderHeight*4];
		
		Vertex posVertex = new Vertex(pos[0], pos[1], pos[2]);
		
		double xRadFov = Math.toRadians(fov/2);
		double yRadFov = Math.toRadians(fov/2) * renderHeight/renderWidth;
		
		double step = Math.PI/renderWidth;
		long startTime = System.currentTimeMillis();
		for (int y = 0; y < renderHeight; y++) {
			for (int x = 0; x < renderWidth; x++) {
				long lastTime = System.currentTimeMillis();
				System.out.printf("Percent Done: %.2f\n", (((y * renderWidth + x)/(renderHeight*renderWidth))*100));
				Ray ray = new Ray(posVertex, new Vector(
						Math.sin(HorLook + xRadFov * -Math.cos(step * x)), 
						Math.sin(VertLook + yRadFov * -Math.cos(step * y)), 
						Math.cos(HorLook + xRadFov * -Math.cos(step * x))), renderDist);
				Color c = ray.getDistColor(ray.march(objs));
				pixels[(y * (int)renderWidth + x) * 4] = c.getRed();
				pixels[(y * (int)renderWidth + x) * 4+1] = c.getGreen();
				pixels[(y * (int)renderWidth + x) * 4+2] = c.getBlue();
				pixels[(y * (int)renderWidth + x) * 4+3] = c.getAlpha();
				System.out.println("Calculation Time: " + (System.currentTimeMillis()-lastTime));
				System.out.println("Total Calculation Time: " + ((System.currentTimeMillis()-startTime)/1000));
			}
		}
		
		return getImageFromArray(pixels, (int)renderWidth, (int)renderHeight);
	}
	
	public static BufferedImage getImageFromArray(int[] pixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WritableRaster raster = (WritableRaster) image.getData();
        raster.setPixels(0,0,width,height,pixels);
        image.setData(raster);
        return image;
    }
	
	public void renderToScreen(Graphics g) {
		updateView();
		SetPrederterminedInfo();

		for (DPolygon poly : polygonList)
			poly.updatePolygon(this);

//		double sunDistance = 100;
//		Cubes.get(0).x = sunDistance * Math.cos(SunPos);
//		Cubes.get(0).z = sunDistance * Math.sin(SunPos);
//		Cubes.get(0).updatePoly();

		// Set drawing order so closest polygons gets drawn last
		setOrder();

		// Set the polygon that the mouse is currently over
		// setPolygonOver();

		// draw polygons in the Order that is set by the 'setOrder' function
		for (int i = 0; i < renderOrder.length; i++)
			polygonList.get(renderOrder[i]).DrawablePolygon.drawPolygon(g);

		// draw the cross in the center of the screen
		drawMouseAim(g);

		// FPS display
		g.drawString(String.format("Camera X: %.2f", pos[0]), 40, 60);
		g.drawString(String.format("Camera Y: %.2f", pos[1]), 40, 80);
		g.drawString(String.format("Camera Z: %.2f", pos[2]), 40, 100);
		g.drawString(String.format("Vertical Orientation: %.2f", VertLook), 200, 60);
		g.drawString(String.format("Horizontal Orientation: %.2f", HorLook), 200, 80);
	}

	public BufferedImage renderImage(double width, double height) {

	    BufferedImage renderedImage = new BufferedImage((int)renderWidth, (int)renderHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = renderedImage.createGraphics();
		
		updateView();
		SetPrederterminedInfo();

		for (DPolygon poly : polygonList)
			poly.updatePolygon(this);

		// Set drawing order so closest polygons gets drawn last
		setOrder();

		// Set the polygon that the mouse is currently over
		// setPolygonOver();

		// draw polygons in the Order that is set by the 'setOrder' function
		for (int i = 0; i < renderOrder.length; i++)
			polygonList.get(renderOrder[i]).DrawablePolygon.drawPolygon(g);
		
		
		renderedImage = centerCropImage(renderedImage, width/height * 0.75);
		renderedImage = resizeImage(renderedImage, (int)width, (int)height);
		
		return renderedImage;
	}
	
	public void drawCameraAim(Graphics g, Camera camera) {
		updateView();
		SetPrederterminedInfo();
		Vertex posVertex = new Vertex(camera.pos[0], camera.pos[1], camera.pos[2]);

		Ray ray = new Ray(posVertex, new Vector(
				Math.sin(camera.HorLook), 
				Math.sin(camera.VertLook*(Math.PI/2)), 
				Math.cos(camera.HorLook)),
				camera.renderDist);
		
		Vertex[] vertices = new Vertex[2];
		vertices[0] = posVertex;
		vertices[1] = ray.getDistVertex();
		
		
		double[] newX = new double[2];
		double[] newY = new double[2];
		boolean draw = true;
		for(int i=0; i< 2; i++)
		{
			double[] CalcPos = CalculatePositionP(pos, viewTo, 
					vertices[i].getX(), vertices[i].getY(), vertices[i].getZ());
			newX[i] = (renderWidth/2 - CalcFocusPos[0]) + CalcPos[0] * zoom;
			newY[i] = (renderHeight/2 - CalcFocusPos[1]) + CalcPos[1] * zoom;			
		}

		if (draw) {
			g.setColor(new Color(0, 0, 0));
			g.drawLine((int)newX[0], (int)newY[0], (int)newX[1], (int)newY[1]);
		}
	}
	
	private BufferedImage resizeImage(BufferedImage img, int newW, int newH) { 
	    Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2d = dimg.createGraphics();
	    g2d.drawImage(tmp, 0, 0, null);
	    g2d.dispose();

	    return dimg;
	}
	
	private BufferedImage cropImage(BufferedImage image, int startX, int startY, int width, int height) { 
		BufferedImage img = image.getSubimage(startX, startY, width, height); //fill in the corners of the desired crop location here
		BufferedImage copyOfImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = copyOfImage.createGraphics();
		g.drawImage(img, 0, 0, null);
		return copyOfImage;
	}
	
	private BufferedImage centerCropImage(BufferedImage image, double widthHeightRatio) {
		double renderRatio = image.getWidth()/image.getHeight();
		int newWidth = (int)image.getWidth(), newHeight = (int)image.getHeight();
		if (renderRatio > widthHeightRatio) {
			newWidth = (int) (image.getWidth() * (widthHeightRatio  / renderRatio));
			newHeight = image.getHeight();
		} else if (widthHeightRatio > renderRatio) {
			newWidth = image.getWidth();
			newHeight = (int) (image.getHeight() * (renderRatio  / widthHeightRatio));
		}
		
		return cropImage(image, image.getWidth()/2 - newWidth/2, image.getHeight()/2 - newHeight/2, newWidth, newHeight);
	}
	
	public void fillPolygonList(ArrayList<DPolygon> polygonList) {
		this.polygonList = polygonList;
	}

	public double[] getPos() {
		return pos;
	}

	public void setPos(double[] pos) {
		this.pos = pos;
	}

	public double[] getViewTo() {
		return viewTo;
	}

	public void setViewTo(double[] viewTo) {
		this.viewTo = viewTo;
	}

	public double getZoom() {
		return zoom;
	}

	public double getFov() {
		return fov;
	}

	void updateView() {
		double r = Math.sqrt(1 - (VertLook * VertLook));
		viewTo = new double[] {pos[0] + r * Math.cos(HorLook),
				pos[1] + r * Math.sin(HorLook),
				pos[2] + VertLook};
	}

	double[] CalculatePositionP(double[] ViewFrom, double[] ViewTo, double x, double y, double z) {
		double[] projP = getProj(ViewFrom, ViewTo, x, y, z, P);
		double[] drawP = getDrawP(projP[0], projP[1], projP[2]);
		return drawP;
	}

	private double[] getProj(double[] ViewFrom, double[] ViewTo, double x, double y, double z, Plane P) {
		Vector ViewToPoint = new Vector(x - ViewFrom[0], y - ViewFrom[1], z - ViewFrom[2]);

		t = (P.NV.x * P.P[0] + P.NV.y * P.P[1] + P.NV.z * P.P[2]
				- (P.NV.x * ViewFrom[0] + P.NV.y * ViewFrom[1] + P.NV.z * ViewFrom[2]))
				/ (P.NV.x * ViewToPoint.x + P.NV.y * ViewToPoint.y + P.NV.z * ViewToPoint.z);

		x = ViewFrom[0] + ViewToPoint.x * t;
		y = ViewFrom[1] + ViewToPoint.y * t;
		z = ViewFrom[2] + ViewToPoint.z * t;

		return new double[] { x, y, z };
	}

	private double[] getDrawP(double x, double y, double z) {
		double DrawX = W2.x * x + W2.y * y + W2.z * z;
		double DrawY = W1.x * x + W1.y * y + W1.z * z;
		return new double[] { DrawX, DrawY };
	}

	private Vector getRotationVector(double[] ViewFrom, double[] ViewTo) {
		double dx = Math.abs(ViewFrom[0] - ViewTo[0]);
		double dy = Math.abs(ViewFrom[1] - ViewTo[1]);
		double xRot, yRot;
		xRot = dy / (dx + dy);
		yRot = dx / (dx + dy);

		if (ViewFrom[1] > ViewTo[1])
			xRot = -xRot;
		if (ViewFrom[0] < ViewTo[0])
			yRot = -yRot;

		Vector V = new Vector(xRot, yRot, 0);
		return V;
	}

	private void SetPrederterminedInfo() {

		ViewVector = new Vector(viewTo[0] - pos[0], viewTo[1] - pos[1], viewTo[2] - pos[2]);
		DirectionVector = new Vector(1, 1, 1);
		PlaneVector1 = ViewVector.CrossProduct(DirectionVector);
		PlaneVector2 = ViewVector.CrossProduct(PlaneVector1);
		P = new Plane(PlaneVector1, PlaneVector2, viewTo);

		RotationVector = getRotationVector(pos, viewTo);
		W1 = ViewVector.CrossProduct(RotationVector);
		W2 = ViewVector.CrossProduct(W1);

		CalcFocusPos = CalculatePositionP(pos, viewTo, viewTo[0], viewTo[1], viewTo[2]);
		CalcFocusPos[0] = zoom * CalcFocusPos[0];
		CalcFocusPos[1] = zoom * CalcFocusPos[1];
	}

	private void setOrder() {
		double[] k = new double[polygonList.size()];
		renderOrder = new int[polygonList.size()];

		for (int i = 0; i < polygonList.size(); i++) {
			k[i] = polygonList.get(i).AvgDist;
			renderOrder[i] = i;
		}

		double temp;
		int tempr;
		for (int a = 0; a < k.length - 1; a++)
			for (int b = 0; b < k.length - 1; b++)
				if (k[b] < k[b + 1]) {
					temp = k[b];
					tempr = renderOrder[b];
					renderOrder[b] = renderOrder[b + 1];
					k[b] = k[b + 1];

					renderOrder[b + 1] = tempr;
					k[b + 1] = temp;
				}
	}

	private void drawMouseAim(Graphics g) {
		g.setColor(Color.black);
		g.drawLine((int) (DDDTutorial.ScreenSize.getWidth() / 2 - aimSight),
				(int) (DDDTutorial.ScreenSize.getHeight() / 2),
				(int) (DDDTutorial.ScreenSize.getWidth() / 2 + aimSight),
				(int) (DDDTutorial.ScreenSize.getHeight() / 2));
		g.drawLine((int) (DDDTutorial.ScreenSize.getWidth() / 2),
				(int) (DDDTutorial.ScreenSize.getHeight() / 2 - aimSight),
				(int) (DDDTutorial.ScreenSize.getWidth() / 2),
				(int) (DDDTutorial.ScreenSize.getHeight() / 2 + aimSight));
	}

	public double getRenderWidth() {
		return renderWidth;
	}

	public double getRenderHeight() {
		return renderHeight;
	}

	public double getVertLook() {
		return VertLook;
	}

	public double getHorLook() {
		return HorLook;
	}

	public double getHorRotSpeed() {
		return HorRotSpeed;
	}

	public double getVertRotSpeed() {
		return VertRotSpeed;
	}

	public void addHorLook(double addition) {
		HorLook += addition;
	}

	public void addVertLook(double addition) {
		VertLook += addition;
	}

	public void setVertLook(double vertLook) {
		VertLook = vertLook;
	}

	public void setHorLook(double horLook) {
		HorLook = horLook;
	}

}
