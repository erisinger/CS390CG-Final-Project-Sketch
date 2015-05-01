package proj;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;

import java.util.ArrayList;

class Hierarchical extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener {

	/* This defines the objModel class, which takes care
	 * of loading a triangular mesh from an obj file,
	 * estimating per vertex average normal,
	 * and displaying the mesh.
	 */
	class objModel {
		public FloatBuffer vertexBuffer;
		public IntBuffer faceBuffer;
		public FloatBuffer normalBuffer;
		public Point3f center;
		public int num_verts;		// number of vertices
		public int num_faces;		// number of triangle faces

		public void Draw() {
			vertexBuffer.rewind();
			normalBuffer.rewind();
			faceBuffer.rewind();
			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
			
			gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
			gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
			
			gl.glDrawElements(GL.GL_TRIANGLES, num_faces*3, GL.GL_UNSIGNED_INT, faceBuffer);
			
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
		}
		
		public objModel(String filename) {
			/* load a triangular mesh model from a .obj file */
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(filename));
			} catch (IOException e) {
				System.out.println("Error reading from file " + filename);
				System.exit(0);
			}

			center = new Point3f();			
			float x, y, z;
			int v1, v2, v3;
			float minx, miny, minz;
			float maxx, maxy, maxz;
			float bbx, bby, bbz;
			minx = miny = minz = 10000.f;
			maxx = maxy = maxz = -10000.f;
			
			String line;
			String[] tokens;
			ArrayList<Point3f> input_verts = new ArrayList<Point3f> ();
			ArrayList<Integer> input_faces = new ArrayList<Integer> ();
			ArrayList<Vector3f> input_norms = new ArrayList<Vector3f> ();
			try {
			while ((line = in.readLine()) != null) {
				if (line.length() == 0)
					continue;
				switch(line.charAt(0)) {
				case 'v':
					tokens = line.split("[ ]+");
					x = Float.valueOf(tokens[1]);
					y = Float.valueOf(tokens[2]);
					z = Float.valueOf(tokens[3]);
					minx = Math.min(minx, x);
					miny = Math.min(miny, y);
					minz = Math.min(minz, z);
					maxx = Math.max(maxx, x);
					maxy = Math.max(maxy, y);
					maxz = Math.max(maxz, z);
					input_verts.add(new Point3f(x, y, z));
					center.add(new Point3f(x, y, z));
					break;
				case 'f':
					String[] f1, f2, f3;
					tokens = line.split("[ ]+");
					f1 = tokens[1].split("/");
					v1 = Integer.valueOf(f1[0]) - 1;
					f2 = tokens[2].split("/");
					v2 = Integer.valueOf(f2[0])-1;
					f3 = tokens[3].split("/");
					v3 = Integer.valueOf(f3[0])-1;
					input_faces.add(v1);
					input_faces.add(v2);
					input_faces.add(v3);				
					break;
				default:
					continue;
				}
			}
			in.close();	
			} catch(IOException e) {
				System.out.println("Unhandled error while reading input file.");
			}

			System.out.println("Read " + input_verts.size() +
						   	" vertices and " + input_faces.size() + " faces.");
			
			center.scale(1.f / (float) input_verts.size());
			
			bbx = maxx - minx;
			bby = maxy - miny;
			bbz = maxz - minz;
			float bbmax = Math.max(bbx, Math.max(bby, bbz));
			
			for (Point3f p : input_verts) {
				
				p.x = (p.x - center.x) / bbmax;
				p.y = (p.y - center.y) / bbmax;
				p.z = (p.z - center.z) / bbmax;
			}
			center.x = center.y = center.z = 0.f;
			
			/* estimate per vertex average normal */
			int i;
			for (i = 0; i < input_verts.size(); i ++) {
				input_norms.add(new Vector3f());
			}
			
			Vector3f e1 = new Vector3f();
			Vector3f e2 = new Vector3f();
			Vector3f tn = new Vector3f();
			for (i = 0; i < input_faces.size(); i += 3) {
				v1 = input_faces.get(i+0);
				v2 = input_faces.get(i+1);
				v3 = input_faces.get(i+2);
				
				e1.sub(input_verts.get(v2), input_verts.get(v1));
				e2.sub(input_verts.get(v3), input_verts.get(v1));
				tn.cross(e1, e2);
				input_norms.get(v1).add(tn);
				
				e1.sub(input_verts.get(v3), input_verts.get(v2));
				e2.sub(input_verts.get(v1), input_verts.get(v2));
				tn.cross(e1, e2);
				input_norms.get(v2).add(tn);
				
				e1.sub(input_verts.get(v1), input_verts.get(v3));
				e2.sub(input_verts.get(v2), input_verts.get(v3));
				tn.cross(e1, e2);
				input_norms.get(v3).add(tn);			
			}

			/* convert to buffers to improve display speed */
			for (i = 0; i < input_verts.size(); i ++) {
				input_norms.get(i).normalize();
			}
			
			vertexBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
			normalBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
			faceBuffer = BufferUtil.newIntBuffer(input_faces.size());
			
			for (i = 0; i < input_verts.size(); i ++) {
				vertexBuffer.put(input_verts.get(i).x);
				vertexBuffer.put(input_verts.get(i).y);
				vertexBuffer.put(input_verts.get(i).z);
				normalBuffer.put(input_norms.get(i).x);
				normalBuffer.put(input_norms.get(i).y);
				normalBuffer.put(input_norms.get(i).z);			
			}
			
			for (i = 0; i < input_faces.size(); i ++) {
				faceBuffer.put(input_faces.get(i));	
			}			
			num_verts = input_verts.size();
			num_faces = input_faces.size()/3;
		}		
	}
	
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_ESCAPE:
		case KeyEvent.VK_Q:
			System.exit(0);
			break;		
		case 'r':
		case 'R':
			initViewParameters();
			break;
		case 'w':
		case 'W':
			wireframe = ! wireframe;
			break;
		case 'b':
		case 'B':
			cullface = !cullface;
			break;
		case 'f':
		case 'F':
			flatshade = !flatshade;
			break;
		case 'a':
		case 'A':
			if (animator.isAnimating())
				animator.stop();
			else 
				animator.start();
			break;
		case '+':
		case '=':
			animation_speed *= 1.2f;
			break;
		case '-':
		case '_':
			animation_speed /= 1.2;
			break;
		case KeyEvent.VK_LEFT:
			movingLeft = !movingLeft;
			movingRight = false;
//			translateLeft();
			break;
		case KeyEvent.VK_RIGHT:
			movingRight = !movingRight;
			movingLeft = false;
//			translateRight();
			break;
		case KeyEvent.VK_SPACE:
			jump();
			break;
		default:
			break;
		
		}
		canvas.display();
	}
	
	/* GL, display, model transformation, and mouse control variables */
	private final GLCanvas canvas;
	private GL gl;
	private final GLU glu = new GLU();	
	private FPSAnimator animator;

	private int winW = 800, winH = 800;
	private boolean wireframe = false;
	private boolean cullface = true;
	private boolean flatshade = false;
	
	private float xpos = 0, ypos = 0, zpos = 0;
	private float centerx, centery, centerz;
	private float roth = 0, rotv = 0;
	private float znear, zfar;
	private int mouseX, mouseY, mouseButton;
	private float motionSpeed, rotateSpeed;
	private float animation_speed = 1.0f;
	
	private float zBack = 5.f;
	
	private float timeStamp = 0;
	private float g = -9.8f;
	
	private boolean movingRight = false;
	private boolean movingLeft = false;
	
	
	
	/* === YOUR WORK HERE === */
	/* Define more models you need for constructing your scene */
	private objModel example_model = new objModel("./a3_objmodels/bunny.obj");
	private objModel cube_model = new objModel("./a3_objmodels/cube.obj");
	public CollisionBox playerToken = new CollisionBox(xpos, ypos, 0, 0.5f, example_model);
	
	private float example_rotateT = 0.f;
	/* Here you should give a conservative estimate of the scene's bounding box
	 * so that the initViewParameters function can calculate proper
	 * transformation parameters to display the initial scene.
	 * If these are not set correctly, the objects may disappear on start.
	 */
	private float xmin = -1f, ymin = -1f, zmin = -1f;
	private float xmax = 1f, ymax = 1f, zmax = 1f;	
	
	private float translateAmt = 0.12f;
	private void translateRight(){
		if (movingRight){
			playerToken.x += translateAmt;
		}
	}
	
	private void translateLeft(){
		if (movingLeft){
			playerToken.x -= translateAmt;
		}
	}
	
	private void jump(){
		if (!playerToken.isFalling()){
			playerToken.y += 1.f;
			playerToken.setFalling(true);
		}
	}
	
	public void display(GLAutoDrawable drawable) {
		
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, wireframe ? GL.GL_LINE : GL.GL_FILL);	
		gl.glShadeModel(flatshade ? GL.GL_FLAT : GL.GL_SMOOTH);		
		if (cullface)
			gl.glEnable(GL.GL_CULL_FACE);
		else
			gl.glDisable(GL.GL_CULL_FACE);		
		
		gl.glLoadIdentity();
		
		/* this is the transformation of the entire scene */
		gl.glTranslatef(-xpos, -ypos, -zpos);
		gl.glTranslatef(centerx, centery, centerz);
		gl.glRotatef(360.f - roth, 0, 1.0f, 0);
		gl.glRotatef(rotv, 1.0f, 0, 0);
		gl.glTranslatef(-centerx, -centery, -centerz);	

		
		/* === YOUR WORK HERE === */
		
		/* Below is an example of a rotating bunny
		 * It rotates the bunny with example_rotateT degrees around the bunny's gravity center  
		 */
		
//		if (playerToken.y < -1.f){
//			new Hierarchical();
//		}
		
		//capture elapsed time since last draw, reset time stamp
		float currentTime = System.currentTimeMillis();
		float elapsed = currentTime - timeStamp;
		timeStamp = currentTime;
		
		translateRight();
		translateLeft();
		
		//effect of gravity on player object
//		playerToken.velocity += g * (elapsed / 1000);
//		playerToken.y -= playerToken.velocity * (elapsed / 1000);
		playerToken.velocity += 0.002f;
		playerToken.y -= playerToken.velocity;
		
		/* design platform -- replace with file-reading functionality */
		ArrayList<CollisionBox> platform = new ArrayList<CollisionBox>();
		
		//"seed" box
		platform.add(new CollisionBox(0, -1.f, 0, 0.5f, cube_model));
		
		//rest of the boxes
		for (int i = 1; i <= 100; i++){
			CollisionBox prev = platform.get(i - 1);
			platform.add(new CollisionBox(prev.x + prev.r + 0.5f, prev.y, prev.z, 0.5f, cube_model));
		}
		
//		//"seed" box
//		platform.add(new CollisionBox(0, -1.f, 0, 0.5f, example_model));
//		
//		//rest of the boxes
//		for (int i = 1; i <= 10; i++){
//			CollisionBox prev = platform.get(i - 1);
//			platform.add(new CollisionBox(prev.x + prev.r + 0.5f, prev.y, prev.z, 0.5f, example_model));
//		}
//		for (int i = 11; i <= 15; i++){
//			CollisionBox a = platform.get(i - 1);
//			platform.add(new CollisionBox(a.x + a.r + 0.5f, a.y, a.z, 0.5f, example_model));
//		}
//		for (int i = 16; i <= 20; i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y + .5f, b.z, 0.5f, example_model));
//		}
//		for(int i = 21 ; i <= 22 ; i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y, b.z, 0.5f, example_model));		
//		}
//		for(int i = 23; i <= 27;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y + -.5f, b.z, 0.5f, example_model));
//		}
//		for(int i = 28; i <= 30;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y , b.z, 0.5f, example_model));
//		}
//		for(int i = 31; i <= 35;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y, b.z, 0.5f, example_model));
//		}
//		
//		for(int i = 36; i <= 39;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, .5f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 40; i <= 43;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, 2f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 44; i <= 47;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, 3.5f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 48; i <= 51;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, 2f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 52; i <= 55;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, .5f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 56; i <= 59;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 60; i <= 63;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -2.5f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 64; i <= 67;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -5f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 68; i <= 71;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -2.5f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 72; i <= 76;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 77; i <= 77;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(75f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 78; i <= 78;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(75f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 79; i <= 85;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 86; i <= 86;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(77f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 87; i <= 87;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(77f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 88; i <= 88;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(79f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 89; i <= 89;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(79f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 90; i <= 90;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(81f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 91; i <= 91;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(81f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 92; i <= 100;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
		
		
		
		//turn a few blocks into pits -- remove from final implementation
//		platform.get(3).makePit(true);
		platform.get(7).makePit(true);
		platform.get(8).makePit(true);
		platform.get(21).makePit(true);
		platform.get(22).makePit(true);
		platform.get(30).makePit(true);
		platform.get(31).makePit(true);
//		platform.get(32).makePit(true);
		platform.get(34).makePit(true);
		platform.get(56).makePit(true);
		platform.get(60).makePit(true);
		platform.get(64).makePit(true);
		platform.get(65).makePit(true);
		platform.get(68).makePit(true);
		platform.get(72).makePit(true);
		//platform.get(77).makePit(true);
		platform.get(79).makePit(true);
		platform.get(81).makePit(true);
		platform.get(83).makePit(true);
		platform.get(85).makePit(true);
		platform.get(92).makePit(true);		
		
//		//turn a few blocks into pits -- remove from final implementation
//		platform.get(3).makePit(true);
//		platform.get(7).makePit(true);
		
		

		gl.glPushMatrix();	// push the current matrix to stack
		
		//background features
		float planetScale = 5.f;
		
		//sun/planet
		gl.glPushMatrix();
		
		gl.glScalef(planetScale, planetScale, planetScale);
		gl.glTranslatef((-playerToken.x / 20.f) /*+ (platform.get(platform.size() - 1).x / 2.f)*/, 0, -5);
		gl.glRotatef(example_rotateT, 0, 1.f, 0);
		
		example_model.Draw();
		
		//satellites
		float satelliteScale = 0.3f;
		gl.glPushMatrix();
		gl.glScalef(satelliteScale, satelliteScale, satelliteScale);
		gl.glTranslatef(5.f, 0, 0);
		gl.glRotatef(example_rotateT / 4.f, 0, 1.f, 0);
		example_model.Draw();
		gl.glPopMatrix();
		
		gl.glPushMatrix();
		gl.glScalef(satelliteScale, satelliteScale, satelliteScale);
		gl.glTranslatef(-5.f, 0, 0);
		gl.glRotatef(example_rotateT / 4.f, 0, 1.f, 0);
		example_model.Draw();
		gl.glPopMatrix();
		
		
		
		//satellites
//		gl.glPushMatrix();
//		gl.glScalef(0.5f, 0.5f, 0.5f);
//		gl.glRotatef(example_rotateT / 4.f, 0, 1.f, 0);
//		gl.glTranslatef(2.f, 0, 0);
//		example_model.Draw();
//		gl.glPopMatrix();
//		
//		gl.glTranslatef(-playerToken.x / 4.f, 0, -5);
//		gl.glScalef(scaleAmt, scaleAmt, scaleAmt);
//		gl.glRotatef(example_rotateT, 0, 1.f, 0);
//		
//		example_model.Draw();
		
		gl.glPopMatrix();
		
		//translate to player token
//		gl.glTranslatef(-translateAmt, playerToken.y, playerToken.z);
		gl.glTranslatef(-playerToken.x, 0, playerToken.z);	
		
		/* draw player token */
		gl.glPushMatrix();
		playerToken.draw3DCollisionBounds(gl);
		gl.glTranslatef(playerToken.x, playerToken.y, 0);
		playerToken.model.Draw();
		gl.glPopMatrix();
		
//		gl.glTranslatef(playerToken.x, playerToken.y, playerToken.z);
		
		/* draw platform */
		for (CollisionBox c : platform){
			gl.glPushMatrix();
			
			
//			gl.glScalef(1.5f, 1.5f, 1.5f);
			
			c.draw3DCollisionBounds(gl);
			gl.glTranslatef(c.x, c.y, c.z);
			
			if (!c.isPit()) c.model.Draw();
			gl.glPopMatrix();
			
		}
		
		gl.glPopMatrix();
		
		//collision checking
		for (CollisionBox c : platform){
			if (!c.isPit() && c.collidesWith(playerToken)){
				
				//collision vertical or horizontal?
				if (c.collidesVertically(playerToken)){
					//vertical reset
					playerToken.y = 0;
					playerToken.velocity = 0;
					playerToken.setFalling(false);
					
//					System.out.println("vertical collision");
				}
//				System.out.println("collision");
				else{
					//horizontal reset
//					playerToken.velocity = 0;
					if (c.collidesFromLeft(playerToken)){
						playerToken.x = c.x - (c.r + playerToken.r);
					}
					else{
						playerToken.x = c.x + c.r + playerToken.r;
					}
					
//					System.out.println("horizontal collision");
				}
				
				
			}
		}		
		
		/* increment example_rotateT */
		if (animator.isAnimating())
			example_rotateT += 1.0f * animation_speed;
		
//		System.out.println("" + playerToken.x + ", " + playerToken.y + ", " + playerToken.z);
	}	
	
	public Hierarchical() {
		super("Assignment 3 -- Hierarchical Modeling");
		canvas = new GLCanvas();
		canvas.addGLEventListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		animator = new FPSAnimator(canvas, 30);	// create a 30 fps animator
		getContentPane().add(canvas);
		setSize(winW, winH);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
		animator.start();
		canvas.requestFocus();
	}
	
	public static void main(String[] args) {

		new Hierarchical();
		
	}
	
	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL();

		initViewParameters();
		gl.glClearColor(.1f, .1f, .1f, 1f);
		gl.glClearDepth(1.0f);

	    // white light at the eye
	    float light0_position[] = { 0, 0, 1, 0 };
	    float light0_diffuse[] = { 1, 1, 1, 1 };
	    float light0_specular[] = { 1, 1, 1, 1 };
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_POSITION, light0_position, 0);
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_DIFFUSE, light0_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_SPECULAR, light0_specular, 0);

	    //red light
	    float light1_position[] = { -.1f, .1f, 0, 0 };
	    float light1_diffuse[] = { .6f, .05f, .05f, 1 };
	    float light1_specular[] = { .6f, .05f, .05f, 1 };
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_POSITION, light1_position, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_DIFFUSE, light1_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_SPECULAR, light1_specular, 0);

	    //blue light
	    float light2_position[] = { .1f, .1f, 0, 0 };
	    float light2_diffuse[] = { .05f, .05f, .6f, 1 };
	    float light2_specular[] = { .05f, .05f, .6f, 1 };
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_POSITION, light2_position, 0);
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_DIFFUSE, light2_diffuse, 0);
	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_SPECULAR, light2_specular, 0);

	    //material
	    float mat_ambient[] = { 0, 0, 0, 1 };
	    float mat_specular[] = { .8f, .8f, .8f, 1 };
	    float mat_diffuse[] = { .4f, .4f, .4f, 1 };
	    float mat_shininess[] = { 128 };
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);

	    float bmat_ambient[] = { 0, 0, 0, 1 };
	    float bmat_specular[] = { 0, .8f, .8f, 1 };
	    float bmat_diffuse[] = { 0, .4f, .4f, 1 };
	    float bmat_shininess[] = { 128 };
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_AMBIENT, bmat_ambient, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SPECULAR, bmat_specular, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_DIFFUSE, bmat_diffuse, 0);
	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SHININESS, bmat_shininess, 0);

	    float lmodel_ambient[] = { 0, 0, 0, 1 };
	    gl.glLightModelfv( GL.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
	    gl.glLightModeli( GL.GL_LIGHT_MODEL_TWO_SIDE, 1 );

	    gl.glEnable( GL.GL_NORMALIZE );
	    gl.glEnable( GL.GL_LIGHTING );
	    gl.glEnable( GL.GL_LIGHT0 );
	    gl.glEnable( GL.GL_LIGHT1 );
	    gl.glEnable( GL.GL_LIGHT2 );

	    gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
		gl.glCullFace(GL.GL_BACK);
		gl.glEnable(GL.GL_CULL_FACE);
		gl.glShadeModel(GL.GL_SMOOTH);		
	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		winW = width;
		winH = height;

		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
			gl.glLoadIdentity();
			glu.gluPerspective(45.f, (float)width/(float)height, znear, zfar);
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}
	
	public void mousePressed(MouseEvent e) {	
		mouseX = e.getX();
		mouseY = e.getY();
		mouseButton = e.getButton();
		canvas.display();
	}
	
	public void mouseReleased(MouseEvent e) {
		mouseButton = MouseEvent.NOBUTTON;
		canvas.display();
	}	
	
	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (mouseButton == MouseEvent.BUTTON3) {
			zpos -= (y - mouseY) * motionSpeed;
			mouseX = x;
			mouseY = y;
			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON2) {
			xpos -= (x - mouseX) * motionSpeed;
			ypos += (y - mouseY) * motionSpeed;
			mouseX = x;
			mouseY = y;
			canvas.display();
		} else if (mouseButton == MouseEvent.BUTTON1) {
			roth -= (x - mouseX) * rotateSpeed;
			rotv += (y - mouseY) * rotateSpeed;
			mouseX = x;
			mouseY = y;
			canvas.display();
		}
	}

	
	/* computes optimal transformation parameters for OpenGL rendering.
	 * this is based on an estimate of the scene's bounding box
	 */	
	void initViewParameters()
	{
		roth = rotv = 0;

		float ball_r = (float) Math.sqrt((xmax-xmin)*(xmax-xmin)
							+ (ymax-ymin)*(ymax-ymin)
							+ (zmax-zmin)*(zmax-zmin)) * 0.707f;

		centerx = (xmax+xmin)/2.f;
		centery = (ymax+ymin)/2.f;
		centerz = (zmax+zmin)/2.f;
		xpos = centerx;
		ypos = centery;
//		zpos = ball_r/(float) Math.sin(45.f*Math.PI/180.f)+centerz;
		zpos = ball_r/(float) Math.sin(45.f*Math.PI/180.f)+centerz + zBack;
		
		znear = 0.01f;
		zfar  = 100000.f;

		motionSpeed = 0.002f * ball_r;
		rotateSpeed = 0.1f;
		
		playerToken = new CollisionBox(xpos, ypos, 0, 0.5f, example_model);
	}	
	
	// these event functions are not used for this assignment
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) { }
	public void keyTyped(KeyEvent e) { }
	public void keyReleased(KeyEvent e) { }
	public void mouseMoved(MouseEvent e) { }
	public void actionPerformed(ActionEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) {	}	
}



//package proj;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.KeyEvent;
//import java.awt.event.KeyListener;
//import java.awt.event.MouseEvent;
//import java.awt.event.MouseListener;
//import java.awt.event.MouseMotionListener;
//import java.io.*;
//import java.nio.FloatBuffer;
//import java.nio.IntBuffer;
//
//import javax.media.opengl.*;
//import javax.media.opengl.glu.GLU;
//import javax.swing.JFrame;
//import javax.vecmath.Point3f;
//import javax.vecmath.Vector3f;
//
//import com.sun.opengl.util.BufferUtil;
//import com.sun.opengl.util.FPSAnimator;
//
//import java.util.ArrayList;
//
//class Hierarchical extends JFrame implements GLEventListener, KeyListener, MouseListener, MouseMotionListener, ActionListener {
//
//	/* This defines the objModel class, which takes care
//	 * of loading a triangular mesh from an obj file,
//	 * estimating per vertex average normal,
//	 * and displaying the mesh.
//	 */
//	class objModel {
//		public FloatBuffer vertexBuffer;
//		public IntBuffer faceBuffer;
//		public FloatBuffer normalBuffer;
//		public Point3f center;
//		public int num_verts;		// number of vertices
//		public int num_faces;		// number of triangle faces
//
//		public void Draw() {
//			vertexBuffer.rewind();
//			normalBuffer.rewind();
//			faceBuffer.rewind();
//			gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
//			gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
//			
//			gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
//			gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);
//			
//			gl.glDrawElements(GL.GL_TRIANGLES, num_faces*3, GL.GL_UNSIGNED_INT, faceBuffer);
//			
//			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
//			gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
//		}
//		
//		public objModel(String filename) {
//			/* load a triangular mesh model from a .obj file */
//			BufferedReader in = null;
//			try {
//				in = new BufferedReader(new FileReader(filename));
//			} catch (IOException e) {
//				System.out.println("Error reading from file " + filename);
//				System.exit(0);
//			}
//
//			center = new Point3f();			
//			float x, y, z;
//			int v1, v2, v3;
//			float minx, miny, minz;
//			float maxx, maxy, maxz;
//			float bbx, bby, bbz;
//			minx = miny = minz = 10000.f;
//			maxx = maxy = maxz = -10000.f;
//			
//			String line;
//			String[] tokens;
//			ArrayList<Point3f> input_verts = new ArrayList<Point3f> ();
//			ArrayList<Integer> input_faces = new ArrayList<Integer> ();
//			ArrayList<Vector3f> input_norms = new ArrayList<Vector3f> ();
//			try {
//			while ((line = in.readLine()) != null) {
//				if (line.length() == 0)
//					continue;
//				switch(line.charAt(0)) {
//				case 'v':
//					tokens = line.split("[ ]+");
//					x = Float.valueOf(tokens[1]);
//					y = Float.valueOf(tokens[2]);
//					z = Float.valueOf(tokens[3]);
//					minx = Math.min(minx, x);
//					miny = Math.min(miny, y);
//					minz = Math.min(minz, z);
//					maxx = Math.max(maxx, x);
//					maxy = Math.max(maxy, y);
//					maxz = Math.max(maxz, z);
//					input_verts.add(new Point3f(x, y, z));
//					center.add(new Point3f(x, y, z));
//					break;
//				case 'f':
//					String[] f1, f2, f3;
//					tokens = line.split("[ ]+");
//					f1 = tokens[1].split("/");
//					v1 = Integer.valueOf(f1[0]) - 1;
//					f2 = tokens[2].split("/");
//					v2 = Integer.valueOf(f2[0])-1;
//					f3 = tokens[3].split("/");
//					v3 = Integer.valueOf(f3[0])-1;
//					input_faces.add(v1);
//					input_faces.add(v2);
//					input_faces.add(v3);				
//					break;
//				default:
//					continue;
//				}
//			}
//			in.close();	
//			} catch(IOException e) {
//				System.out.println("Unhandled error while reading input file.");
//			}
//
//			System.out.println("Read " + input_verts.size() +
//						   	" vertices and " + input_faces.size() + " faces.");
//			
//			center.scale(1.f / (float) input_verts.size());
//			
//			bbx = maxx - minx;
//			bby = maxy - miny;
//			bbz = maxz - minz;
//			float bbmax = Math.max(bbx, Math.max(bby, bbz));
//			
//			for (Point3f p : input_verts) {
//				
//				p.x = (p.x - center.x) / bbmax;
//				p.y = (p.y - center.y) / bbmax;
//				p.z = (p.z - center.z) / bbmax;
//			}
//			center.x = center.y = center.z = 0.f;
//			
//			/* estimate per vertex average normal */
//			int i;
//			for (i = 0; i < input_verts.size(); i ++) {
//				input_norms.add(new Vector3f());
//			}
//			
//			Vector3f e1 = new Vector3f();
//			Vector3f e2 = new Vector3f();
//			Vector3f tn = new Vector3f();
//			for (i = 0; i < input_faces.size(); i += 3) {
//				v1 = input_faces.get(i+0);
//				v2 = input_faces.get(i+1);
//				v3 = input_faces.get(i+2);
//				
//				e1.sub(input_verts.get(v2), input_verts.get(v1));
//				e2.sub(input_verts.get(v3), input_verts.get(v1));
//				tn.cross(e1, e2);
//				input_norms.get(v1).add(tn);
//				
//				e1.sub(input_verts.get(v3), input_verts.get(v2));
//				e2.sub(input_verts.get(v1), input_verts.get(v2));
//				tn.cross(e1, e2);
//				input_norms.get(v2).add(tn);
//				
//				e1.sub(input_verts.get(v1), input_verts.get(v3));
//				e2.sub(input_verts.get(v2), input_verts.get(v3));
//				tn.cross(e1, e2);
//				input_norms.get(v3).add(tn);			
//			}
//
//			/* convert to buffers to improve display speed */
//			for (i = 0; i < input_verts.size(); i ++) {
//				input_norms.get(i).normalize();
//			}
//			
//			vertexBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
//			normalBuffer = BufferUtil.newFloatBuffer(input_verts.size()*3);
//			faceBuffer = BufferUtil.newIntBuffer(input_faces.size());
//			
//			for (i = 0; i < input_verts.size(); i ++) {
//				vertexBuffer.put(input_verts.get(i).x);
//				vertexBuffer.put(input_verts.get(i).y);
//				vertexBuffer.put(input_verts.get(i).z);
//				normalBuffer.put(input_norms.get(i).x);
//				normalBuffer.put(input_norms.get(i).y);
//				normalBuffer.put(input_norms.get(i).z);			
//			}
//			
//			for (i = 0; i < input_faces.size(); i ++) {
//				faceBuffer.put(input_faces.get(i));	
//			}			
//			num_verts = input_verts.size();
//			num_faces = input_faces.size()/3;
//		}		
//	}
//	
//	public void keyPressed(KeyEvent e) {
//		switch(e.getKeyCode()) {
//		case KeyEvent.VK_ESCAPE:
//		case KeyEvent.VK_Q:
//			System.exit(0);
//			break;		
//		case 'r':
//		case 'R':
//			initViewParameters();
//			break;
//		case 'w':
//		case 'W':
//			wireframe = ! wireframe;
//			break;
//		case 'b':
//		case 'B':
//			cullface = !cullface;
//			break;
//		case 'f':
//		case 'F':
//			flatshade = !flatshade;
//			break;
//		case 'a':
//		case 'A':
//			if (animator.isAnimating())
//				animator.stop();
//			else 
//				animator.start();
//			break;
//		case '+':
//		case '=':
//			animation_speed *= 1.2f;
//			break;
//		case '-':
//		case '_':
//			animation_speed /= 1.2;
//			break;
//		case KeyEvent.VK_LEFT:
//			translateLeft();
//			break;
//		case KeyEvent.VK_RIGHT:
//			translateRight();
//			break;
//		case KeyEvent.VK_SPACE:
//			translateUp();
//			break;
//		default:
//			break;
//		}
//		canvas.display();
//	}
//	
//	/* GL, display, model transformation, and mouse control variables */
//	private final GLCanvas canvas;
//	private GL gl;
//	private final GLU glu = new GLU();	
//	private FPSAnimator animator;
//
//	private int winW = 800, winH = 800;
//	private boolean wireframe = false;
//	private boolean cullface = true;
//	private boolean flatshade = false;
//	
//	private float xpos = 0, ypos = 0, zpos = 0;
//	private float centerx, centery, centerz;
//	private float roth = 0, rotv = 0;
//	private float znear, zfar;
//	private int mouseX, mouseY, mouseButton;
//	private float motionSpeed, rotateSpeed;
//	private float animation_speed = 1.0f;
//	
//	private float translateAmt = 0;
//	private float translateVert = 0;
//	private float zBack = 5.f;
//	
//	private float timeStamp = 0;
//	private float g = -9.8f;
//	
//	
//	
//	/* === YOUR WORK HERE === */
//	/* Define more models you need for constructing your scene */
//	private objModel example_model = new objModel("./a3_objmodels/cube.obj");
////	private objModel fireballs = new objModel("Sphere.obj");
//	private objModel bunny = new objModel("./a3_objmodels/bunny.obj");
//	ArrayList<CollisionBox> platform = new ArrayList<CollisionBox>();
//	CollisionBox playerToken = new CollisionBox(xpos, ypos, 0, 0.5f, example_model);
//
//
//	
//	private float example_rotateT = 0.f;
//	/* Here you should give a conservative estimate of the scene's bounding box
//	 * so that the initViewParameters function can calculate proper
//	 * transformation parameters to display the initial scene.
//	 * If these are not set correctly, the objects may disappear on start.
//	 */
//	private float xmin = -3f, ymin = -3f, zmin = -3f;
//	private float xmax = 3f, ymax = 3f, zmax = 3f;	
//	
//	private void translateRight(){
//		translateAmt += 1f;
//	}
//	
//	private void translateLeft(){
//		translateAmt -= 1f;
//	}
//	
//	private void translateUp(){
//		translateVert += -0.5f;
//	}
//	
//	
//public void display(GLAutoDrawable drawable) {
//		
//		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
//		
//		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, wireframe ? GL.GL_LINE : GL.GL_FILL);	
//		gl.glShadeModel(flatshade ? GL.GL_FLAT : GL.GL_SMOOTH);		
//		if (cullface)
//			gl.glEnable(GL.GL_CULL_FACE);
//		else
//			gl.glDisable(GL.GL_CULL_FACE);		
//		
//		gl.glLoadIdentity();
//		
//		/* this is the transformation of the entire scene */
//		gl.glTranslatef(-xpos, -ypos, -zpos);
//		gl.glTranslatef(centerx, centery, centerz);
//		gl.glRotatef(360.f - roth, 0, 1.0f, 0);
//		gl.glRotatef(rotv, 1.0f, 0, 0);
//		gl.glTranslatef(-centerx, -centery, -centerz);	
//
//		
//		/* === YOUR WORK HERE === */
//		
//		/* Below is an example of a rotating bunny
//		 * It rotates the bunny with example_rotateT degrees around the bunny's gravity center  
//		 */
//		
//		//capture elapsed time since last draw, reset time stamp
//		float currentTime = System.currentTimeMillis();
//		float elapsed = currentTime - timeStamp;
//		timeStamp = currentTime;
//		
//		//player object
//		CollisionBox playerToken = new CollisionBox(xpos, ypos, 0, 0.5f, bunny);
//		playerToken.velocity = playerToken.velocity + g * (elapsed / 1000);
//		playerToken.y -= playerToken.velocity * (elapsed / 1000);
//		
//		/* design platform -- replace with file-reading functionality */
//		ArrayList<CollisionBox> platform = new ArrayList<CollisionBox>();
//		
//		
//		//"seed" box
//		platform.add(new CollisionBox(0, -1.f, 0, 0.5f, example_model));
//		
//		//rest of the boxes
//		for (int i = 1; i <= 10; i++){
//			CollisionBox prev = platform.get(i - 1);
//			platform.add(new CollisionBox(prev.x + prev.r + 0.5f, prev.y, prev.z, 0.5f, example_model));
//		}
//		for (int i = 11; i <= 15; i++){
//			CollisionBox a = platform.get(i - 1);
//			platform.add(new CollisionBox(a.x + a.r + 0.5f, a.y, a.z, 0.5f, example_model));
//		}
//		for (int i = 16; i <= 20; i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y + .5f, b.z, 0.5f, example_model));
//		}
//		for(int i = 21 ; i <= 22 ; i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y, b.z, 0.5f, example_model));		
//		}
//		for(int i = 23; i <= 27;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y + -.5f, b.z, 0.5f, example_model));
//		}
//		for(int i = 28; i <= 30;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y , b.z, 0.5f, example_model));
//		}
//		for(int i = 31; i <= 35;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, b.y, b.z, 0.5f, example_model));
//		}
//		
//		for(int i = 36; i <= 39;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, .5f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 40; i <= 43;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, 2f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 44; i <= 47;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, 3.5f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 48; i <= 51;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, 2f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 52; i <= 55;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, .5f, b.z, 0.5f, example_model));	
//		}
//		for(int i = 56; i <= 59;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 60; i <= 63;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -2.5f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 64; i <= 67;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -5f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 68; i <= 71;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -2.5f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 72; i <= 76;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 77; i <= 77;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(75f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 78; i <= 78;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(75f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 79; i <= 85;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 86; i <= 86;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(77f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 87; i <= 87;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(77f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 88; i <= 88;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(79f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 89; i <= 89;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(79f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 90; i <= 90;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(81f + b.r + 0.5f, 0 , b.z, 0.5f, example_model));	
//			
//		}
//		for(int i = 91; i <= 91;i++){ // vertical cubes 
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(81f + b.r + 0.5f, 1f , b.z, 0.5f, example_model));	
//		}
//		for(int i = 92; i <= 100;i++){
//			CollisionBox b = platform.get(i - 1);
//			platform.add(new CollisionBox(b.x + b.r + 0.5f, -1f , b.z, 0.5f, example_model));	
//		}
//		
//		
//		
//		//turn a few blocks into pits -- remove from final implementation
//	//	platform.get(3).makePit(true);
//		platform.get(7).makePit(true);
//		platform.get(21).makePit(true);
//		platform.get(30).makePit(true);
//		platform.get(32).makePit(true);
//		platform.get(34).makePit(true);
//		platform.get(56).makePit(true);
//		platform.get(60).makePit(true);
//		platform.get(64).makePit(true);
//		platform.get(65).makePit(true);
//		platform.get(68).makePit(true);
//		platform.get(72).makePit(true);
//		//platform.get(77).makePit(true);
//		platform.get(79).makePit(true);
//		platform.get(81).makePit(true);
//		platform.get(83).makePit(true);
//		platform.get(85).makePit(true);
//		platform.get(92).makePit(true);
//
//
//
//		
//		//translate to player token
//		gl.glTranslatef(-translateAmt, playerToken.y, playerToken.z);
////		gl.glTranslatef(xpos, playerToken.y, playerToken.z);
//
//		gl.glPushMatrix();	// push the current matrix to stack
//		
//		gl.glPushMatrix();
//		gl.glRotatef(example_rotateT, 0, 1, 0);
//		gl.glPopMatrix();
//		
//		/* draw player token */
//		gl.glPushMatrix();
//		gl.glTranslatef(translateAmt, -translateVert, 0);
//		
//		//player token
//		playerToken.model.Draw();
//		playerToken.draw3DCollisionBounds(gl);
//		gl.glPopMatrix();
//		
//		gl.glTranslatef(playerToken.x, playerToken.y, playerToken.z);
//		
//		gl.glPushMatrix();
//		
//		/* draw platform */
//		for (CollisionBox c : platform){
//			gl.glPushMatrix();
//			gl.glTranslatef(c.x, c.y, c.z);
//			if (!c.isPit()) c.model.Draw();
//			gl.glPopMatrix();
//			c.draw3DCollisionBounds(gl);
//			
//		}
//		gl.glPopMatrix();
//		
//		gl.glPopMatrix();
//		
//		//primitive pit collision checking
//		for (CollisionBox c : platform){
////			System.out.println("" + xpos + ", " + ypos);
////			System.out.println("" + c.x + ", " + c.y + "; " + playerToken.x + ", " + playerToken.y);
//			if (c.isPit() && playerToken.x > c.x - c.r - translateAmt && playerToken.x < c.x + c.r - translateAmt){
//				System.out.println("pit collision");
//			}
//		}
//		
//		/* increment example_rotateT */
//		if (animator.isAnimating())
//			example_rotateT += 1.0f * animation_speed;
//		
////		System.out.println("" + playerToken.x + ", " + playerToken.y + ", " + playerToken.z);
//	}	
//	
//	public Hierarchical() {
//		super("Assignment 3 -- Hierarchical Modeling");
//		canvas = new GLCanvas();
//		canvas.addGLEventListener(this);
//		canvas.addKeyListener(this);
//		canvas.addMouseListener(this);
//		canvas.addMouseMotionListener(this);
//		animator = new FPSAnimator(canvas, 30);	// create a 30 fps animator
//		getContentPane().add(canvas);
//		setSize(winW, winH);
//		setLocationRelativeTo(null);
//		setDefaultCloseOperation(EXIT_ON_CLOSE);
//		setVisible(true);
//		animator.start();
//		canvas.requestFocus();
//		
//			
//	}
//	
//	public static void main(String[] args) {
//
//		new Hierarchical();
//	}
//	
//	public void init(GLAutoDrawable drawable) {
//		gl = drawable.getGL();
//
//		initViewParameters();
//		gl.glClearColor(.1f, .1f, .1f, 1f);
//		gl.glClearDepth(1.0f);
//
//	    // white light at the eye
//	    float light0_position[] = { 0, 0, 1, 0 };
//	    float light0_diffuse[] = { 1, 1, 1, 1 };
//	    float light0_specular[] = { 1, 1, 1, 1 };
//	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_POSITION, light0_position, 0);
//	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_DIFFUSE, light0_diffuse, 0);
//	    gl.glLightfv( GL.GL_LIGHT0, GL.GL_SPECULAR, light0_specular, 0);
//
//	    //red light
//	    float light1_position[] = { -.1f, .1f, 0, 0 };
//	    float light1_diffuse[] = { .6f, .05f, .05f, 1 };
//	    float light1_specular[] = { .6f, .05f, .05f, 1 };
//	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_POSITION, light1_position, 0);
//	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_DIFFUSE, light1_diffuse, 0);
//	    gl.glLightfv( GL.GL_LIGHT1, GL.GL_SPECULAR, light1_specular, 0);
//
//	    //blue light
//	    float light2_position[] = { .1f, .1f, 0, 0 };
//	    float light2_diffuse[] = { .05f, .05f, .6f, 1 };
//	    float light2_specular[] = { .05f, .05f, .6f, 1 };
//	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_POSITION, light2_position, 0);
//	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_DIFFUSE, light2_diffuse, 0);
//	    gl.glLightfv( GL.GL_LIGHT2, GL.GL_SPECULAR, light2_specular, 0);
//
//	    //material
//	    float mat_ambient[] = { 0, 0, 0, 1 };
//	    float mat_specular[] = { .8f, .8f, .8f, 1 };
//	    float mat_diffuse[] = { .4f, .4f, .4f, 1 };
//	    float mat_shininess[] = { 128 };
//	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
//	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
//	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
//	    gl.glMaterialfv( GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);
//
//	    float bmat_ambient[] = { 0, 0, 0, 1 };
//	    float bmat_specular[] = { 0, .8f, .8f, 1 };
//	    float bmat_diffuse[] = { 0, .4f, .4f, 1 };
//	    float bmat_shininess[] = { 128 };
//	    gl.glMaterialfv( GL.GL_BACK, GL.GL_AMBIENT, bmat_ambient, 0);
//	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SPECULAR, bmat_specular, 0);
//	    gl.glMaterialfv( GL.GL_BACK, GL.GL_DIFFUSE, bmat_diffuse, 0);
//	    gl.glMaterialfv( GL.GL_BACK, GL.GL_SHININESS, bmat_shininess, 0);
//
//	    float lmodel_ambient[] = { 0, 0, 0, 1 };
//	    gl.glLightModelfv( GL.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
//	    gl.glLightModeli( GL.GL_LIGHT_MODEL_TWO_SIDE, 1 );
//
//	    gl.glEnable( GL.GL_NORMALIZE );
//	    gl.glEnable( GL.GL_LIGHTING );
//	    gl.glEnable( GL.GL_LIGHT0 );
//	    gl.glEnable( GL.GL_LIGHT1 );
//	    gl.glEnable( GL.GL_LIGHT2 );
//
//	    gl.glEnable(GL.GL_DEPTH_TEST);
//		gl.glDepthFunc(GL.GL_LESS);
//		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
//		gl.glCullFace(GL.GL_BACK);
//		gl.glEnable(GL.GL_CULL_FACE);
//		gl.glShadeModel(GL.GL_SMOOTH);		
//	}
//	
//	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
//		winW = width;
//		winH = height;
//
//		gl.glViewport(0, 0, width, height);
//		gl.glMatrixMode(GL.GL_PROJECTION);
//			gl.glLoadIdentity();
//			glu.gluPerspective(45.f, (float)width/(float)height, znear, zfar);
//		gl.glMatrixMode(GL.GL_MODELVIEW);
//	}
//	
//	public void mousePressed(MouseEvent e) {	
//		mouseX = e.getX();
//		mouseY = e.getY();
//		mouseButton = e.getButton();
//		canvas.display();
//	}
//	
//	public void mouseReleased(MouseEvent e) {
//		mouseButton = MouseEvent.NOBUTTON;
//		canvas.display();
//	}	
//	
//	public void mouseDragged(MouseEvent e) {
//		int x = e.getX();
//		int y = e.getY();
//		if (mouseButton == MouseEvent.BUTTON3) {
//			zpos -= (y - mouseY) * motionSpeed;
//			mouseX = x;
//			mouseY = y;
//			canvas.display();
//		} else if (mouseButton == MouseEvent.BUTTON2) {
//			xpos -= (x - mouseX) * motionSpeed;
//			ypos += (y - mouseY) * motionSpeed;
//			mouseX = x;
//			mouseY = y;
//			canvas.display();
//		} else if (mouseButton == MouseEvent.BUTTON1) {
//			roth -= (x - mouseX) * rotateSpeed;
//			rotv += (y - mouseY) * rotateSpeed;
//			mouseX = x;
//			mouseY = y;
//			canvas.display();
//		}
//	}
//
//	
//	/* computes optimal transformation parameters for OpenGL rendering.
//	 * this is based on an estimate of the scene's bounding box
//	 */	
//	void initViewParameters()
//	{
//		roth = rotv = 0;
//
//		float ball_r = (float) Math.sqrt((xmax-xmin)*(xmax-xmin)
//							+ (ymax-ymin)*(ymax-ymin)
//							+ (zmax-zmin)*(zmax-zmin)) * 0.707f;
//
//		centerx = (xmax+xmin)/2.f;
//		centery = (ymax+ymin)/2.f;
//		centerz = (zmax+zmin)/2.f;
//		xpos = centerx;
//		ypos = centery;
////		zpos = ball_r/(float) Math.sin(45.f*Math.PI/180.f)+centerz;
//		zpos = ball_r/(float) Math.sin(45.f*Math.PI/180.f)+centerz + zBack;
//		
//		znear = 0.01f;
//		zfar  = 1000.f;
//
//		motionSpeed = 0.002f * ball_r;
//		rotateSpeed = 0.1f;
//
//	}	
//	
//	// these event functions are not used for this assignment
//	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) { }
//	public void keyTyped(KeyEvent e) { }
//	public void keyReleased(KeyEvent e) { }
//	public void mouseMoved(MouseEvent e) { }
//	public void actionPerformed(ActionEvent e) { }
//	public void mouseClicked(MouseEvent e) { }
//	public void mouseEntered(MouseEvent e) { }
//	public void mouseExited(MouseEvent e) {	}	
//}