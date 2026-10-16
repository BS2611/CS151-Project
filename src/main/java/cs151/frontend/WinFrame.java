package cs151.frontend;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.stage.Stage;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gives the frameless JavaFX window the native behaviour of a normal Windows window: Aero Snap (drag to a screen
 * edge, Win+arrow keys), the Windows 11 snap-layouts pop-up over the maximise button, native resize borders,
 * native window drag by the top bar, and a window shadow.
 *
 * <p>A JavaFX undecorated window is created without the "resizable / maximisable" window styles, which is why snap
 * does not work out of the box. This class adds those styles and takes over a few window messages (calling the
 * original JavaFX handler for everything else). It uses the Java foreign function API, so no extra library is
 * needed. If anything fails, {@link #install(Stage)} returns false and the app falls back to the Java-only
 * move/resize in {@link WindowChrome}.
 */
public final class WinFrame {

    // window messages
    private static final int WM_SIZE = 0x0005;
    private static final int WM_NCCALCSIZE = 0x0083;
    private static final int WM_NCHITTEST = 0x0084;
    private static final int WM_NCMOUSEMOVE = 0x00A0;
    private static final int WM_NCLBUTTONDOWN = 0x00A1;
    private static final int WM_NCLBUTTONUP = 0x00A2;
    private static final int WM_NCMOUSELEAVE = 0x02A2;

    // hit-test results
    private static final long HTCLIENT = 1;
    private static final long HTCAPTION = 2;
    private static final long HTMAXBUTTON = 9;
    private static final long HTLEFT = 10;
    private static final long HTRIGHT = 11;
    private static final long HTTOP = 12;
    private static final long HTTOPLEFT = 13;
    private static final long HTTOPRIGHT = 14;
    private static final long HTBOTTOM = 15;
    private static final long HTBOTTOMLEFT = 16;
    private static final long HTBOTTOMRIGHT = 17;

    private static final int GWL_STYLE = -16;
    private static final int GWLP_WNDPROC = -4;
    private static final long WS_CAPTION = 0x00C00000L;
    private static final long WS_SYSMENU = 0x00080000L;
    private static final long WS_THICKFRAME = 0x00040000L;
    private static final long WS_MINIMIZEBOX = 0x00020000L;
    private static final long WS_MAXIMIZEBOX = 0x00010000L;

    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_NOACTIVATE = 0x0010;
    private static final int SWP_FRAMECHANGED = 0x0020;

    private static final int SW_MAXIMIZE = 3;
    private static final int SW_RESTORE = 9;

    private static final int SM_CXSIZEFRAME = 32;
    private static final int SM_CYSIZEFRAME = 33;
    private static final int SM_CXPADDEDBORDER = 92;

    /** Width of the invisible resize border, in device-independent pixels. */
    private static final double BORDER = 6;

    private static MethodHandle getWindowLongPtr;
    private static MethodHandle setWindowLongPtr;
    private static MethodHandle setWindowPos;
    private static MethodHandle callWindowProc;
    private static MethodHandle getWindowRect;
    private static MethodHandle isZoomed;
    private static MethodHandle showWindow;
    private static MethodHandle getSystemMetrics;
    private static MethodHandle trackMouseEvent;

    private static MemorySegment rect;
    private static MemorySegment trackEvent;

    private static long hwnd;
    private static long originalProc;
    private static Stage stage;
    private static boolean installed;

    // what the hit test needs to know about the JavaFX layout
    private static Node topbar;
    private static Node maximizeButton;
    private static List<Node> noDragNodes = List.of();
    private static Runnable stateListener = () -> { };
    private static boolean maxHover;

    private WinFrame() {
    }

    public static boolean isInstalled() {
        return installed;
    }

    /** Tells the hit test where the draggable top bar and the maximise button are (called by the top bar). */
    public static void setLayoutNodes(Node bar, Node maxButton, List<Node> noDrag) {
        topbar = bar;
        maximizeButton = maxButton;
        noDragNodes = List.copyOf(noDrag);
    }

    /** Called (on the FX thread) whenever the window is maximised or restored. */
    public static void setStateListener(Runnable listener) {
        stateListener = listener;
    }

    public static boolean isMaximized() {
        try {
            return installed && (int) isZoomed.invoke(hwnd) != 0;
        } catch (Throwable error) {
            return false;
        }
    }

    public static void toggleMaximize() {
        try {
            showWindow.invoke(hwnd, isMaximized() ? SW_RESTORE : SW_MAXIMIZE);
        } catch (Throwable ignored) {
            // nothing sensible to do: the buttons simply do nothing
        }
    }

    /** Hooks the native window of the (already shown) stage. Returns false when this is not Windows or it failed. */
    public static boolean install(Stage window) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return false;
        }
        try {
            stage = window;
            Linker linker = Linker.nativeLinker();
            Arena arena = Arena.global();
            SymbolLookup user32 = SymbolLookup.libraryLookup("user32", arena);

            ValueLayout.OfLong j = ValueLayout.JAVA_LONG;
            ValueLayout.OfInt i = ValueLayout.JAVA_INT;

            MethodHandle findWindowEx = linker.downcallHandle(user32.find("FindWindowExW").orElseThrow(),
                    FunctionDescriptor.of(j, j, j, j, j));
            MethodHandle getWindowThreadProcessId = linker.downcallHandle(user32.find("GetWindowThreadProcessId").orElseThrow(),
                    FunctionDescriptor.of(i, j, j));
            getWindowLongPtr = linker.downcallHandle(user32.find("GetWindowLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(j, j, i));
            setWindowLongPtr = linker.downcallHandle(user32.find("SetWindowLongPtrW").orElseThrow(),
                    FunctionDescriptor.of(j, j, i, j));
            setWindowPos = linker.downcallHandle(user32.find("SetWindowPos").orElseThrow(),
                    FunctionDescriptor.of(i, j, j, i, i, i, i, i));
            callWindowProc = linker.downcallHandle(user32.find("CallWindowProcW").orElseThrow(),
                    FunctionDescriptor.of(j, j, j, i, j, j));
            getWindowRect = linker.downcallHandle(user32.find("GetWindowRect").orElseThrow(),
                    FunctionDescriptor.of(i, j, j));
            isZoomed = linker.downcallHandle(user32.find("IsZoomed").orElseThrow(),
                    FunctionDescriptor.of(i, j));
            showWindow = linker.downcallHandle(user32.find("ShowWindow").orElseThrow(),
                    FunctionDescriptor.of(i, j, i));
            getSystemMetrics = linker.downcallHandle(user32.find("GetSystemMetrics").orElseThrow(),
                    FunctionDescriptor.of(i, i));
            trackMouseEvent = linker.downcallHandle(user32.find("TrackMouseEvent").orElseThrow(),
                    FunctionDescriptor.of(i, j));

            rect = arena.allocate(16, 8);
            trackEvent = arena.allocate(24, 8);

            MemorySegment title = arena.allocateFrom(window.getTitle(), StandardCharsets.UTF_16LE);
            // Several copies of the app may be open: take the window with this title that belongs to this process.
            MemorySegment pid = arena.allocate(4, 4);
            long ownPid = ProcessHandle.current().pid();
            long candidate = 0;
            hwnd = 0;
            while ((candidate = (long) findWindowEx.invoke(0L, candidate, 0L, title.address())) != 0) {
                getWindowThreadProcessId.invoke(candidate, pid.address());
                if (pid.get(ValueLayout.JAVA_INT, 0) == ownPid) {
                    hwnd = candidate;
                    break;
                }
            }
            if (hwnd == 0) {
                return false;
            }

            MethodHandle target = MethodHandles.lookup().findStatic(WinFrame.class, "windowProc",
                    MethodType.methodType(long.class, long.class, int.class, long.class, long.class));
            MemorySegment stub = linker.upcallStub(target, FunctionDescriptor.of(j, j, i, j, j), arena);

            originalProc = (long) setWindowLongPtr.invoke(hwnd, GWLP_WNDPROC, stub.address());
            if (originalProc == 0) {
                return false;
            }

            long style = (long) getWindowLongPtr.invoke(hwnd, GWL_STYLE);
            style |= WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MINIMIZEBOX | WS_MAXIMIZEBOX;
            setWindowLongPtr.invoke(hwnd, GWL_STYLE, style);
            setWindowPos.invoke(hwnd, 0L, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED);

            installed = true;
            return true;
        } catch (Throwable error) {
            installed = false;
            return false;
        }
    }

    /** The window procedure that replaces JavaFX's; handles a few messages and forwards the rest. */
    private static long windowProc(long window, int message, long wParam, long lParam) {
        try {
            switch (message) {
                case WM_NCCALCSIZE -> {
                    // no native title bar or border: the client area is the whole window
                    if (wParam != 0 && isMaximized()) {
                        MemorySegment params = MemorySegment.ofAddress(lParam).reinterpret(16);
                        int padded = (int) getSystemMetrics.invoke(SM_CXPADDEDBORDER);
                        int fx = (int) getSystemMetrics.invoke(SM_CXSIZEFRAME) + padded;
                        int fy = (int) getSystemMetrics.invoke(SM_CYSIZEFRAME) + padded;
                        params.set(ValueLayout.JAVA_INT, 0, params.get(ValueLayout.JAVA_INT, 0) + fx);
                        params.set(ValueLayout.JAVA_INT, 4, params.get(ValueLayout.JAVA_INT, 4) + fy);
                        params.set(ValueLayout.JAVA_INT, 8, params.get(ValueLayout.JAVA_INT, 8) - fx);
                        params.set(ValueLayout.JAVA_INT, 12, params.get(ValueLayout.JAVA_INT, 12) - fy);
                    }
                    return 0;
                }
                case WM_NCHITTEST -> {
                    long hit = hitTest(window, lParam);
                    if (hit != HTCLIENT) {
                        return hit;
                    }
                }
                case WM_NCMOUSEMOVE -> {
                    if (wParam == HTMAXBUTTON) {
                        setMaxHover(true);
                        trackLeave(window);
                    } else {
                        setMaxHover(false);
                    }
                }
                case WM_NCMOUSELEAVE -> setMaxHover(false);
                case WM_NCLBUTTONDOWN -> {
                    if (wParam == HTMAXBUTTON) {
                        return 0; // handled on button up
                    }
                }
                case WM_NCLBUTTONUP -> {
                    if (wParam == HTMAXBUTTON) {
                        toggleMaximize();
                        return 0;
                    }
                }
                default -> {
                }
            }
            long result = (long) callWindowProc.invoke(originalProc, window, message, wParam, lParam);
            if (message == WM_SIZE) {
                stateListener.run();
            }
            return result;
        } catch (Throwable error) {
            try {
                return (long) callWindowProc.invoke(originalProc, window, message, wParam, lParam);
            } catch (Throwable fatal) {
                return 0;
            }
        }
    }

    private static long hitTest(long window, long lParam) throws Throwable {
        int screenX = (short) (lParam & 0xFFFF);
        int screenY = (short) ((lParam >> 16) & 0xFFFF);
        getWindowRect.invoke(window, rect.address());
        int left = rect.get(ValueLayout.JAVA_INT, 0);
        int top = rect.get(ValueLayout.JAVA_INT, 4);
        int right = rect.get(ValueLayout.JAVA_INT, 8);
        int bottom = rect.get(ValueLayout.JAVA_INT, 12);

        double scale = stage.getOutputScaleX() <= 0 ? 1 : stage.getOutputScaleX();
        double x = (screenX - left) / scale;
        double y = (screenY - top) / scale;
        double width = (right - left) / scale;
        double height = (bottom - top) / scale;

        if (x < 0 || y < 0 || x > width || y > height) {
            return HTCLIENT;
        }

        if (!isMaximized()) {
            boolean west = x < BORDER;
            boolean east = x > width - BORDER;
            boolean north = y < BORDER;
            boolean south = y > height - BORDER;
            if (north && west) {
                return HTTOPLEFT;
            }
            if (north && east) {
                return HTTOPRIGHT;
            }
            if (south && west) {
                return HTBOTTOMLEFT;
            }
            if (south && east) {
                return HTBOTTOMRIGHT;
            }
            if (west) {
                return HTLEFT;
            }
            if (east) {
                return HTRIGHT;
            }
            if (north) {
                return HTTOP;
            }
            if (south) {
                return HTBOTTOM;
            }
        }

        if (contains(maximizeButton, x, y)) {
            return HTMAXBUTTON;
        }
        if (contains(topbar, x, y)) {
            for (Node node : noDragNodes) {
                if (contains(node, x, y)) {
                    return HTCLIENT;
                }
            }
            return HTCAPTION;
        }
        return HTCLIENT;
    }

    private static boolean contains(Node node, double x, double y) {
        if (node == null || node.getScene() == null || !node.isVisible()) {
            return false;
        }
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return bounds != null && bounds.contains(x, y);
    }

    private static void trackLeave(long window) throws Throwable {
        trackEvent.set(ValueLayout.JAVA_INT, 0, 24);          // cbSize
        trackEvent.set(ValueLayout.JAVA_INT, 4, 0x02 | 0x10); // TME_LEAVE | TME_NONCLIENT
        trackEvent.set(ValueLayout.JAVA_LONG, 8, window);     // hwndTrack
        trackEvent.set(ValueLayout.JAVA_INT, 16, 0);          // dwHoverTime
        trackMouseEvent.invoke(trackEvent.address());
    }

    /** The maximise button never receives normal mouse events (the system owns that area), so show its hover by hand. */
    private static void setMaxHover(boolean hover) {
        if (hover == maxHover) {
            return;
        }
        maxHover = hover;
        if (maximizeButton != null) {
            if (hover) {
                maximizeButton.getStyleClass().add("native-hover");
            } else {
                maximizeButton.getStyleClass().remove("native-hover");
            }
        }
    }
}
