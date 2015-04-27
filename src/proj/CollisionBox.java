package proj;

import javax.media.opengl.GL;

import proj.Hierarchical.objModel;

 class CollisionBox{
 
	objModel model;
	float x;
	float y;
	float z;
	float r;
	float velocity;
	
	private boolean isPit = false;
	
	public  CollisionBox(objModel box){
		model = box;	
	}
	
	public CollisionBox(float x , float y, float z, float r, objModel box){
		this.x = x;
		this.y = y;
		this.z = z;
		this.r = r;
		model = box;
	}
	
	public boolean collidesWith(CollisionBox c){
		
		//2D AABB equations
		if (Math.abs(this.x - c.x) < this.r + c.r){
			if (Math.abs(this.y - c.y) < this.r + c.r){
				return true;
			}
		}
		return false;
	}
	
	public boolean isPit(){
		return isPit;
	}
	
	public void makePit(boolean p){
		isPit = p;
	}
	
	public void draw2DCollisionBounds(GL gl){
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y - r, 0);
		gl.glVertex3f(x + r, y - r, 0);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y - r, 0);
		gl.glVertex3f(x + r, y + r, 0);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y + r, 0);
		gl.glVertex3f(x - r, y + r, 0);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y + r, 0);
		gl.glVertex3f(x - r, y - r, 0);
		gl.glEnd();
	}
	
	public void draw3DCollisionBounds(GL gl){
		//draw 3D box representing collision boundaries
		
		//front
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y - r, z + r);
		gl.glVertex3f(x + r, y - r, z + r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y - r, z + r);
		gl.glVertex3f(x + r, y + r, z + r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y + r, z + r);
		gl.glVertex3f(x - r, y + r, z + r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y + r, z + r);
		gl.glVertex3f(x - r, y - r, z + r);
		gl.glEnd();
		
		//back
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y - r, z - r);
		gl.glVertex3f(x + r, y - r, z - r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y - r, z - r);
		gl.glVertex3f(x + r, y + r, z - r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y + r, z - r);
		gl.glVertex3f(x - r, y + r, z - r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y + r, z - r);
		gl.glVertex3f(x - r, y - r, z - r);
		gl.glEnd();
		
		//join front square to back square
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y - r, z + r);
		gl.glVertex3f(x - r, y - r, z - r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y - r, z + r);
		gl.glVertex3f(x + r, y - r, z - r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x + r, y + r, z + r);
		gl.glVertex3f(x + r, y + r, z - r);
		gl.glEnd();
		
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(x - r, y + r, z + r);
		gl.glVertex3f(x - r, y + r, z - r);
		gl.glEnd();
	}

}
 
 
