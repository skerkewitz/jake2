package jake2.sys;

import jake2.client.Key;
import jake2.render.opengl.LwjglDriver;
import org.lwjgl.glfw.GLFWKeyCallback;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Created by tropper on 18.05.17.
 */
public class GlfwKBDImpl extends KBD {

    private int last_mx;
    private int last_my;

    @Override
    public void Init() {
        long window_ = LwjglDriver.window;
        glfwSetKeyCallback(window_, (window, key, scancode, action, mods) -> {
            System.out.println("key " + key);
            Do_Key_Event(XLateKey(key, scancode), action == GLFW_PRESS);
        });

        glfwSetCharCallback(window_, (window, codepoint) -> {
            System.out.println("codepoint " + codepoint);
        });

        glfwSetMouseButtonCallback(window_, (window, button, action, mods) -> {
            System.out.println("mousebutton " + button);

            int key = mouseEventToKey(button);
            Do_Key_Event(key, true);
        });

        glfwSetCursorPosCallback(window_, (window, xpos, ypos) -> {
            System.out.println("mouse pos " + xpos + " " + ypos);
            last_mx = (int) xpos;
            last_my = (int) ypos;
        });
    }

    private final int mouseEventToKey(int button) {
        switch (button) {
            case 2:
                return Key.K_MOUSE2;
            case 1:
                return Key.K_MOUSE3;
            default:
                return Key.K_MOUSE1;
        }
    }

    @Override
    public void Update() {
//        HandleEvents();

        double[] l_mx = new double[1];
        double[] l_my = new double[1];

        glfwGetCursorPos(LwjglDriver.window, l_mx, l_my);

        if (IN.mouse_active) {
            mx = (int) ((l_mx[0] - 640 / 2) * 2);
            my = (int) ((l_my[0] - 480 / 2) * 2);
        } else {
            mx = 0;
            my = 0;
        }

        glfwSetCursorPos(LwjglDriver.window, 320, 240);
    }

//    private void HandleEvents() {
//        int key;
//
//        Jake2InputEvent event;
//        while ( (event=InputListener.nextEvent()) != null ) {
//            switch(event.type) {
//                case Jake2InputEvent.KeyPress:
//                case Jake2InputEvent.KeyRelease:
//
//                    break;
//
//                case Jake2InputEvent.MotionNotify:
////					if (IN.ignorefirst) {
////						IN.ignorefirst = false;
////						break;
////					}
//                    if (IN.mouse_active) {
//                        mx = (((MouseEvent)event.ev).getX() - win_w2) * 2;
//                        my = (((MouseEvent)event.ev).getY() - win_h2) * 2;
//                    } else {
//                        mx = 0;
//                        my = 0;
//                    }
//                    break;
//                // see java.awt.MouseEvent
//                case Jake2InputEvent.ButtonPress:
//                    key = mouseEventToKey((MouseEvent)event.ev);
//                    Do_Key_Event(key, true);
//                    break;
//
//                case Jake2InputEvent.ButtonRelease:
//                    key = mouseEventToKey((MouseEvent)event.ev);
//                    Do_Key_Event(key, false);
//                    break;
//
//                case Jake2InputEvent.WheelMoved:
//                    int dir = ((MouseWheelEvent)event.ev).getWheelRotation();
//                    if (dir > 0) {
//                        Do_Key_Event(Key.K_MWHEELDOWN, true);
//                        Do_Key_Event(Key.K_MWHEELDOWN, false);
//                    } else {
//                        Do_Key_Event(Key.K_MWHEELUP, true);
//                        Do_Key_Event(Key.K_MWHEELUP, false);
//                    }
//                    break;
//
//                case Jake2InputEvent.CreateNotify :
//                case Jake2InputEvent.ConfigureNotify :
//                    handleCreateAndConfigureNotify(((ComponentEvent)event.ev).getComponent());
//                    break;
//            }
//        }
//
//        if (mx != 0 || my != 0) {
//            // move the mouse to the window center again
//            robot.mouseMove(win_x + win_w2, win_y + win_h2);
//        }
//    }

    @Override
    public void Close() {

    }

    @Override
    public void Do_Key_Event(int key, boolean down) {
        Key.Event(key, down, Timer.Milliseconds());
    }

    @Override
    public void installGrabs() {
        glfwSetInputMode(LwjglDriver.window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    @Override
    public void uninstallGrabs() {
        glfwSetInputMode(LwjglDriver.window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    private static int XLateKey(int code, int scancode) {

        int key = 0;

        switch (code) {

            case GLFW_KEY_PAGE_UP:
                return Key.K_PGUP;
            case GLFW_KEY_PAGE_DOWN:
                return Key.K_PGDN;
            case GLFW_KEY_HOME:
                return Key.K_HOME;

            case GLFW_KEY_END:
                return Key.K_END;

//            case GLFW_KEY_.VK_KP_LEFT: key = Key.K_KP_LEFTARROW; break;
            case GLFW_KEY_LEFT:
                return Key.K_LEFTARROW;

//            case GLFW_KEY_.VK_KP_RIGHT: key = Key.K_KP_RIGHTARROW; break;
            case GLFW_KEY_RIGHT:
                return Key.K_RIGHTARROW;

//            case GLFW_KEY_.VK_KP_DOWN:return Key.K_KP_DOWNARROW;
            case GLFW_KEY_DOWN:
                return Key.K_DOWNARROW;

//            case GLFW_KEY_.VK_KP_UP:return Key.K_KP_UPARROW;
            case GLFW_KEY_UP:
                return Key.K_UPARROW;

            case GLFW_KEY_ESCAPE:
                return Key.K_ESCAPE;

            case GLFW_KEY_ENTER:
                return Key.K_ENTER;
//	00652                 case XK_KP_Enter:return K_KP_ENTER;

//            case KeyEvent.VK_TAB:return Key.K_TAB;
//
//            case KeyEvent.VK_F1:return Key.K_F1;
//            case KeyEvent.VK_F2:return Key.K_F2;
//            case KeyEvent.VK_F3:return Key.K_F3;
//            case KeyEvent.VK_F4:return Key.K_F4;
//            case KeyEvent.VK_F5:return Key.K_F5;
//            case KeyEvent.VK_F6:return Key.K_F6;
//            case KeyEvent.VK_F7:return Key.K_F7;
//            case KeyEvent.VK_F8:return Key.K_F8;
//            case KeyEvent.VK_F9:return Key.K_F9;
//            case KeyEvent.VK_F10:return Key.K_F10;
//            case KeyEvent.VK_F11:return Key.K_F11;
//            case KeyEvent.VK_F12:return Key.K_F12;
//
//            case KeyEvent.VK_BACK_SPACE:return Key.K_BACKSPACE;
//
//            case KeyEvent.VK_DELETE:return Key.K_DEL;
////	00683                 case XK_KP_Delete:return K_KP_DEL; break;
//
//            case KeyEvent.VK_PAUSE:return Key.K_PAUSE; break;
//
//            case KeyEvent.VK_SHIFT: key = Key.K_SHIFT; break;
//            case KeyEvent.VK_CONTROL: key = Key.K_CTRL; break;
//
//            case KeyEvent.VK_ALT:
//            case KeyEvent.VK_ALT_GRAPH: key = Key.K_ALT; break;
//
////	00700                 case XK_KP_Begin: key = K_KP_5; break;
////	00701
//            case KeyEvent.VK_INSERT: key = Key.K_INS; break;
//            // toggle console for DE and US keyboards
//            case KeyEvent.VK_DEAD_ACUTE:
//            case KeyEvent.VK_CIRCUMFLEX:
//            case KeyEvent.VK_DEAD_CIRCUMFLEX: key = '`'; break;

            default:
                key = scancode;
                if (key >= 'A' && key <= 'Z')
                    key = key - 'A' + 'a';
                break;
        }
        if (key > 255) key = 0;

        return key;
    }
}


//
//
//
//final public class JOGLKBD extends KBD
//{
//
//
//
//    public void Update() {
//        // get events
//        HandleEvents();
//    }
//
//    public void Close() {
//    }
//
//    private void HandleEvents()
//    {
//        int key;
//
//        Jake2InputEvent event;
//        while ( (event=InputListener.nextEvent()) != null ) {
//            switch(event.type) {
//                case Jake2InputEvent.KeyPress:
//                case Jake2InputEvent.KeyRelease:
//                    Do_Key_Event(XLateKey((KeyEvent)event.ev), event.type == Jake2InputEvent.KeyPress);
//                    break;
//
//                case Jake2InputEvent.MotionNotify:
////					if (IN.ignorefirst) {
////						IN.ignorefirst = false;
////						break;
////					}
//                    if (IN.mouse_active) {
//                        mx = (((MouseEvent)event.ev).getX() - win_w2) * 2;
//                        my = (((MouseEvent)event.ev).getY() - win_h2) * 2;
//                    } else {
//                        mx = 0;
//                        my = 0;
//                    }
//                    break;
//                // see java.awt.MouseEvent
//                case Jake2InputEvent.ButtonPress:
//
//                    break;
//
//                case Jake2InputEvent.ButtonRelease:
//                    key = mouseEventToKey((MouseEvent)event.ev);
//                    Do_Key_Event(key, false);
//                    break;
//
//                case Jake2InputEvent.WheelMoved:
//                    int dir = ((MouseWheelEvent)event.ev).getWheelRotation();
//                    if (dir > 0) {
//                        Do_Key_Event(Key.K_MWHEELDOWN, true);
//                        Do_Key_Event(Key.K_MWHEELDOWN, false);
//                    } else {
//                        Do_Key_Event(Key.K_MWHEELUP, true);
//                        Do_Key_Event(Key.K_MWHEELUP, false);
//                    }
//                    break;
//
//                case Jake2InputEvent.CreateNotify :
//                case Jake2InputEvent.ConfigureNotify :
//                    handleCreateAndConfigureNotify(((ComponentEvent)event.ev).getComponent());
//                    break;
//            }
//        }
//
//        if (mx != 0 || my != 0) {
//            // move the mouse to the window center again
//            robot.mouseMove(win_x + win_w2, win_y + win_h2);
//        }
//    }
//
//    private static void handleCreateAndConfigureNotify(Component component) {
//        // Probably could unify this code better, but for now just
//        // leave the two code paths separate
//        if (!Globals.appletMode) {
//            win_x = 0;
//            win_y = 0;
//            win_w2 = component.getWidth() / 2;
//            win_h2 = component.getHeight() / 2;
//            int left = 0; int top = 0;
//            while (component != null) {
//                if (component instanceof Container) {
//                    Insets insets = ((Container)component).getInsets();
//                    left += insets.left;
//                    top += insets.top;
//                }
//                win_x += component.getX();
//                win_y += component.getY();
//                component = component.getParent();
//            }
//            win_x += left; win_y += top;
//            win_w2 -= left / 2; win_h2 -= top / 2;
//        } else {
//            win_x = 0;
//            win_y = 0;
//            win_w2 = component.getWidth() / 2;
//            win_h2 = component.getHeight() / 2;
//            Point p = component.getLocationOnScreen();
//            win_x = p.x;
//            win_y = p.y;
//        }
//    }
//
//    // strange button numbering in java.awt.MouseEvent
//    // BUTTON1(left) BUTTON2(center) BUTTON3(right)
//    // K_MOUSE1      K_MOUSE3        K_MOUSE2
//    private final int mouseEventToKey(MouseEvent ev) {
//        switch (ev.getButton()) {
//            case MouseEvent.BUTTON3:
//                return Key.K_MOUSE2;
//            case MouseEvent.BUTTON2:
//                return Key.K_MOUSE3;
//            default:
//                return Key.K_MOUSE1;
//        }
//    }
//
//
//
//    public void Do_Key_Event(int key, boolean down) {
//        Key.Event(key, down, Timer.Milliseconds());
//    }
//
//    public void centerMouse() {
//        robot.mouseMove(win_x + win_w2, win_y + win_h2);
//    }
//
//    public void installGrabs()
//    {
//        if (emptyCursor == null) {
//            ImageIcon emptyIcon = new ImageIcon(new byte[0]);
//            emptyCursor = c.getToolkit().createCustomCursor(emptyIcon.getImage(), new Point(0, 0), "emptyCursor");
//        }
//        c.setCursor(emptyCursor);
//        centerMouse();
//    }
//
//    public void uninstallGrabs()
//    {
//        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
//    }
//}
//
