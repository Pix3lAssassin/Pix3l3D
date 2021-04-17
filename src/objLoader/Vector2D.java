package objLoader;

public class Vector2D {
	public double x, y;
	public Vector2D(double x, double y)
	{
		double Length = Math.sqrt(x*x + y*y);

		if(Length>0)
		{
			this.x = x/Length;
			this.y = y/Length;
		}

	}
	
	public Vector2D getPerpendicularVector() {
		return new Vector2D(1, -x/y);
	}
	
	public double dotProduct(Vector2D v)
	{
		return (x * v.getX()) + (y * v.getY());		
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

}
