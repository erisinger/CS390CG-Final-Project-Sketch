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
	private boolean falling = false;
	public boolean isGoal = false;
	
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
		if (overlapsVertically(c) && overlapsHorizontally(c)){
				return true;
			}
		return false;
	}
	
	public boolean collidesVertically(CollisionBox c){
		return Math.abs(this.y - c.y) > Math.abs(this.x - c.x) + 0.05f && c.y > this.y;
	}
	
	public boolean collidesFromLeft(CollisionBox c){
		return this.x > c.x;
	}
	
	public boolean overlapsVertically(CollisionBox c){
		if (Math.abs(this.y - c.y) < this.r + c.r){
			return true;
		}
		return false;
	}
	
	public boolean overlapsHorizontally(CollisionBox c){
		if (Math.abs(this.x - c.x) < this.r + c.r){
			return true;
		}
		return false;		
	}
	
	public boolean isFalling(){
		return falling;
	}
	
	public void setFalling(boolean f){
		falling = f;
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
 
 
