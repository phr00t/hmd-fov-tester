/*

    - need to get right camera matrix being sent to shader

*/

package mygame;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.util.SkyFactory;
import jmevr.app.VRApplication;
import jmevr.input.VRBounds;
import jmevr.post.CartoonSSAO;
import jmevr.util.VRGuiManager;
import jmevr.util.VRGuiManager.POSITIONING_MODE;

/**
 *
 * @author reden
 */
public class Main extends VRApplication {

    // set some VR settings & start the app
    public static void main(String[] args){
        Main test = new Main();
        test.preconfigureVRApp(VRApplication.PRECONFIG_PARAMETER.USE_CUSTOM_DISTORTION, false); // use full screen distortion, maximum FOV, possibly quicker even
        test.preconfigureVRApp(VRApplication.PRECONFIG_PARAMETER.FORCE_VR_MODE, true); // render two eyes, regardless of SteamVR
        test.preconfigureVRApp(VRApplication.PRECONFIG_PARAMETER.SET_GUI_CURVED_SURFACE, true);
        test.preconfigureVRApp(VRApplication.PRECONFIG_PARAMETER.FLIP_EYES, false);
        test.preconfigureVRApp(VRApplication.PRECONFIG_PARAMETER.SET_GUI_OVERDRAW, true); // show gui even if it is behind things
        test.preconfigureVRApp(VRApplication.PRECONFIG_PARAMETER.INSTANCE_VR_RENDERING, false); // WIP
        test.preconfigureVRApp(VRApplication.PRECONFIG_PARAMETER.NO_GUI, false);
        test.preconfigureFrustrumNearFar(0.1f, 512f);
        test.start();
    }
    
    Spatial observer;
    Material mat;
    BitmapText bt;
    Geometry box;
    
    @Override
    public void simpleInitApp() {        
        initTestScene();
    }
    
    private void initTestScene(){
        observer = new Node("observer");
        
        Spatial sky = SkyFactory.createSky(
                    assetManager, "Textures/Sky/Bright/spheremap.png", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        
        box = new Geometry("", new Box(1,1,1));
        mat = new Material(getAssetManager(), "jmevr/shaders/Unshaded.j3md");
        Texture noise = getAssetManager().loadTexture("Textures/noise.png");
        noise.setMagFilter(MagFilter.Nearest);
        noise.setMinFilter(MinFilter.Trilinear);
        noise.setAnisotropicFilter(16);
        box.setMaterial(mat);
        box.setCullHint(CullHint.Never);
        mat.setTexture("ColorMap", noise);
        box.setLocalTranslation(0f, 0f, 15f);
        rootNode.attachChild(box);
        
        // add FOV number to GUI
        BitmapFont bf = null;
        try {
             bf = assetManager.loadFont("Interface/Fonts/FuturedBlack.fnt");
        } catch(Exception e) { }
        bt = new BitmapText(bf);
        bt.setSize(64f);
        Vector2f size = VRGuiManager.getCanvasSize();
        bt.setLocalTranslation(size.x*0.4f, size.y*0.4f, 1f);
        guiNode.attachChild(bt);
                     
        // make the floor according to the size of our play area
        Geometry floor = new Geometry("floor", new Box(1f, 1f, 1f));
        Vector2f playArea = VRBounds.getPlaySize();
        if( playArea == null ) {
            // no play area, use default size & height
            floor.setLocalScale(2f, 0.5f, 2f);
            floor.move(0f, -1.5f, 0f);
        } else {
            // cube model is actually 2x as big, cut it down to proper playArea size with * 0.5
            floor.setLocalScale(playArea.x * 0.5f, 0.5f, playArea.y * 0.5f);
            floor.move(0f, -0.5f, 0f);
        }
        floor.setMaterial(mat);
        rootNode.attachChild(floor);
        
        // test any positioning mode here (defaults to AUTO_CAM_ALL)
        VRGuiManager.setPositioningMode(POSITIONING_MODE.AUTO_CAM_ALL);
        VRGuiManager.setGuiScale(0.4f);
        
        observer.setLocalTranslation(new Vector3f(0.0f, 0.0f, 0.0f));
        
        VRApplication.setObserver(observer);
        rootNode.attachChild(observer);
        
        initInputs();
        
        // use magic VR mouse cusor (same usage as non-VR mouse cursor)
        inputManager.setCursorVisible(true);
    }

     private void initInputs() {
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("incShift", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("decShift", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("filter", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("dumpImages", new KeyTrigger(KeyInput.KEY_I));
        ActionListener acl = new ActionListener() {

            public void onAction(String name, boolean keyPressed, float tpf) {
                if(name.equals("incShift") && keyPressed){
                    VRGuiManager.adjustGuiDistance(-0.1f);
                }else if(name.equals("decShift") && keyPressed){
                    VRGuiManager.adjustGuiDistance(0.1f);
                }else if(name.equals("filter") && keyPressed){
                    // adding filters in realtime
                    CartoonSSAO cartfilt = new CartoonSSAO();
                    FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
                    fpp.addFilter(cartfilt);
                    viewPort.addProcessor(fpp);
                    // filters added to main viewport during runtime,
                    // move them into VR processing
                    // (won't do anything if not in VR mode)
                    VRApplication.moveScreenProcessingToVR();
                }
                if( name.equals("toggle") ) {
                    VRGuiManager.positionGui();
                }                
                
                
            }
        };
        inputManager.addListener(acl, "forward");
        inputManager.addListener(acl, "back");
        inputManager.addListener(acl, "left");
        inputManager.addListener(acl, "right");
        inputManager.addListener(acl, "toggle");
        inputManager.addListener(acl, "incShift");
        inputManager.addListener(acl, "decShift");
        inputManager.addListener(acl, "filter");
        inputManager.addListener(acl, "dumpImages");
    }
    
    private static final Quaternion tempq = new Quaternion();
    private static final Vector3f tempf = new Vector3f();
    public static Quaternion GetWorldRotation(Quaternion wanted, Quaternion relativeTo) {
        tempq.set(relativeTo).inverseLocal().multLocal(wanted);
        return tempq;
    }
    
    public static float FastAngleBetweenXZ(Vector3f vector, Vector3f otherVector) {
        return (float)Math.atan2(vector.z - otherVector.z, vector.x - otherVector.x) * 180f / 3.14159265f;
    }
    
    public static float AngleOfVectorXZ(Vector3f vector) {
        return (float)Math.atan2(vector.z, vector.x) * 180f / 3.14159265f;
    }
    
    public static float FastAngleBetweenYX(Vector3f vector, Vector3f otherVector) {
        return (float)Math.atan2(vector.y - otherVector.y, vector.z - otherVector.z) * 180f / 3.14159265f;
    }
    
    public static float AngleOfVectorYX(Vector3f vector) {
        return (float)Math.atan2(vector.y, vector.z) * 180f / 3.14159265f;
    }
    
    public static float GetRelativeAngleXZ(Vector3f sourcePos, Quaternion sourceLook, Vector3f target) {
        float xz = FastAngleBetweenXZ(target, sourcePos) - AngleOfVectorXZ(sourceLook.getRotationColumn(2, tempf));        
        return Math.abs(xz);
    }
    
    public static float GetRelativeAngleYX(Vector3f sourcePos, Quaternion sourceLook, Vector3f target) {
        float yx = FastAngleBetweenYX(target, sourcePos) - AngleOfVectorYX(sourceLook.getRotationColumn(2, tempf));        
        return Math.abs(yx);
    }
    
    @Override
    public void simpleUpdate(float tpf){
        // update FOV number
        Quaternion youlook = VRApplication.getFinalObserverRotation();
        Vector3f youpos = VRApplication.getFinalObserverPosition();
        int xz = Math.round(GetRelativeAngleXZ(youpos, youlook, box.getLocalTranslation()));
        int yx = Math.round(GetRelativeAngleYX(youpos, youlook, box.getLocalTranslation()));
        bt.setText("Deg FOV X: " + Integer.toString(xz) + "\n" + "Deg FOV Y: " + Integer.toString(yx));
    }
}
