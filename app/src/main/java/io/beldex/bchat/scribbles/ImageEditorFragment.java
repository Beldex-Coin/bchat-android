package io.beldex.bchat.scribbles;

import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import io.beldex.bchat.R;
import io.beldex.bchat.imageeditor.ColorableRenderer;
import io.beldex.bchat.imageeditor.ImageEditorView;
import io.beldex.bchat.imageeditor.Renderer;
import io.beldex.bchat.imageeditor.model.EditorElement;
import io.beldex.bchat.imageeditor.model.EditorModel;
import io.beldex.bchat.imageeditor.renderers.EmojiRenderer;
import io.beldex.bchat.imageeditor.renderers.TextRenderer;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.mediasend.MediaSendPageFragment;
import io.beldex.bchat.mms.MediaConstraints;
import io.beldex.bchat.mms.PushMediaConstraints;
import io.beldex.bchat.util.ParcelUtil;
import com.beldex.libbchat.utilities.TextSecurePreferences;

import androidx.core.graphics.Insets;

public final class ImageEditorFragment extends Fragment implements ImageEditorHud.EventListener,
        MediaSendPageFragment {

  private static final String TAG = Log.tag(ImageEditorFragment.class);

  private static final String KEY_IMAGE_URI = "image_uri";

  private static final int MAX_CROP_RESET_STEPS = 100;

  private EditorModel restoredModel;

  @Nullable
  private EditorElement currentSelection;
  private int           imageMaxHeight;
  private int           imageMaxWidth;
  private boolean       undoAvailable;

  @Nullable
  private byte[]              modeSnapshot;
  private ImageEditorHud.Mode currentMode = ImageEditorHud.Mode.NONE;

  public static class Data {
    private final Bundle bundle;

    Data(Bundle bundle) {
      this.bundle = bundle;
    }

    public Data() {
      this(new Bundle());
    }

    void writeModel(@NonNull EditorModel model) {
      byte[] bytes = ParcelUtil.serialize(model);
      bundle.putByteArray("MODEL", bytes);
    }

    @Nullable
    public EditorModel readModel() {
      byte[] bytes = bundle.getByteArray("MODEL");
      if (bytes == null) {
        return null;
      }
      return ParcelUtil.deserialize(bytes, EditorModel.CREATOR);
    }
  }

  private Uri             imageUri;
  private Controller      controller;
  private ImageEditorHud  imageEditorHud;
  private ImageEditorView imageEditorView;

  public static ImageEditorFragment newInstance(@NonNull Uri imageUri) {
    Bundle args = new Bundle();
    args.putParcelable(KEY_IMAGE_URI, imageUri);

    ImageEditorFragment fragment = new ImageEditorFragment();
    fragment.setArguments(args);
    fragment.setUri(imageUri);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!(getActivity() instanceof Controller)) {
      throw new IllegalStateException("Parent activity must implement Controller interface.");
    }
    controller = (Controller) getActivity();
    Bundle arguments = getArguments();
    if (arguments != null) {
      imageUri = arguments.getParcelable(KEY_IMAGE_URI);
    }

    if (imageUri == null) {
      throw new AssertionError("No KEY_IMAGE_URI supplied");
    }

    MediaConstraints mediaConstraints = new PushMediaConstraints();

    imageMaxWidth  = mediaConstraints.getImageMaxWidth(requireContext());
    imageMaxHeight = mediaConstraints.getImageMaxHeight(requireContext());
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.image_editor_fragment, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    imageEditorHud  = view.findViewById(R.id.scribble_hud);
    imageEditorView = view.findViewById(R.id.image_editor_view);

    imageEditorHud.setEventListener(this);

    View scribbleCloseButton = view.findViewById(R.id.scribble_close_button);
    scribbleCloseButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

    imageEditorView.setTapListener(selectionListener);
    imageEditorView.setUndoRedoStackListener(this::onUndoRedoAvailabilityChanged);

    EditorModel editorModel = null;

    if (restoredModel != null) {
      editorModel = restoredModel;
      restoredModel = null;
    } else if (savedInstanceState != null) {
      editorModel = new Data(savedInstanceState).readModel();
    }

    if (editorModel == null) {
      editorModel = new EditorModel();
      EditorElement image = new EditorElement(new UriGlideRenderer(imageUri, true, imageMaxWidth, imageMaxHeight));
      image.getFlags().setSelectable(false).persist();
      editorModel.addElement(image);
    }

    imageEditorView.setModel(editorModel);

    ViewCompat.setOnApplyWindowInsetsListener(imageEditorHud, (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      Insets ime        = insets.getInsets(WindowInsetsCompat.Type.ime());
      v.setPadding(systemBars.left, 0, systemBars.right, 0);
      imageEditorHud.setBottomInset(Math.max(systemBars.bottom, ime.bottom));
      return insets;
    });

    ViewCompat.setOnApplyWindowInsetsListener(imageEditorView, (v, insets) -> {
      int topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top / 2;
      FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) v.getLayoutParams();
      if (layoutParams.topMargin != topMargin) {
        layoutParams.topMargin = topMargin;
        v.setLayoutParams(layoutParams);
      }
      return insets;
    });
    ViewCompat.requestApplyInsets(imageEditorView);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    new Data(outState).writeModel(imageEditorView.getModel());
  }

  @Override
  public void setUri(@NonNull Uri uri) {
    this.imageUri = uri;
  }

  @NonNull
  @Override
  public Uri getUri() {
    return imageUri;
  }

  @Nullable
  @Override
  public View getPlaybackControls() {
    return null;
  }

  @Override
  public Object saveState() {
    Data data = new Data();
    data.writeModel(imageEditorView.getModel());
    return data;
  }

  @Override
  public void restoreState(@NonNull Object state) {
    if (state instanceof Data) {

      Data        data  = (Data) state;
      EditorModel model = data.readModel();

      if (model != null) {
        if (imageEditorView != null) {
          imageEditorView.setModel(model);
        } else {
          this.restoredModel = model;
        }
      }
    } else {
      Log.w(TAG, "Received a bad saved state. Received class: " + state.getClass().getName());
    }
  }

  private void changeEntityColor(int selectedColor) {
    if (currentSelection != null) {
      Renderer renderer = currentSelection.getRenderer();
      if (renderer instanceof ColorableRenderer) {
        ((ColorableRenderer) renderer).setColor(selectedColor);
      }
    }
  }

  private void startTextEntityEditing(@NonNull EditorElement textElement, boolean selectAll) {
    imageEditorView.startTextEditing(textElement, TextSecurePreferences.isIncognitoKeyboardEnabled(requireContext()), selectAll);
  }

  protected void addText() {
    String        initialText = "";
    int           color       = imageEditorHud.getActiveColor();
    TextRenderer  renderer    = new TextRenderer(initialText, color);
    EditorElement element     = new EditorElement(renderer);

    imageEditorView.getModel().addElementCentered(element, 1);
    imageEditorView.invalidate();

    currentSelection = element;

    startTextEntityEditing(element, true);
  }

  @Override
  public void onBottomPanelHeightChanged(int height) {
    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageEditorView.getLayoutParams();
    if (params.bottomMargin != height) {
      params.bottomMargin = height;
      imageEditorView.setLayoutParams(params);
    }
  }

  @Override
  public void onEmojiSelected(@NonNull String emoji) {
    EditorElement element = new EditorElement(new EmojiRenderer(emoji));
    imageEditorView.getModel().addElementCentered(element, 0.2f);
    imageEditorView.invalidate();
    currentSelection = element;
  }

  private void enterHudMode(@NonNull ImageEditorHud.Mode mode) {
    trackMode(mode);
    imageEditorHud.enterMode(mode);
  }

  private void trackMode(@NonNull ImageEditorHud.Mode mode) {
    if (mode == ImageEditorHud.Mode.NONE) {
      modeSnapshot = null;
    } else if (currentMode == ImageEditorHud.Mode.NONE) {
      modeSnapshot = ParcelUtil.serialize(imageEditorView.getModel());
    }
    currentMode = mode;
  }

  @Override
  public void onModeStarted(@NonNull ImageEditorHud.Mode mode) {
    trackMode(mode);
    imageEditorView.setMode(ImageEditorView.Mode.MoveAndResize);
    imageEditorView.doneTextEditing();

    controller.onTouchEventsNeeded(mode != ImageEditorHud.Mode.NONE);

    switch (mode) {
      case CROP:
        imageEditorView.getModel().startCrop();
      break;

      case DRAW:
        imageEditorView.startDrawing(0.01f, Paint.Cap.ROUND);
        break;

      case HIGHLIGHT:
        imageEditorView.startDrawing(0.03f, Paint.Cap.SQUARE);
        break;

      case TEXT:
        addText();
        break;


      case NONE:
        imageEditorView.getModel().doneCrop();
        currentSelection = null;
        break;
    }
  }

  @Override
  public void onColorChange(int color) {
    imageEditorView.setDrawingBrushColor(color);
    changeEntityColor(color);
  }

  @Override
  public void onUndo() {
    imageEditorView.getModel().undo();
  }

  @Override
  public void onRedo() {
    imageEditorView.getModel().redo();
  }

  @Override
  public void onDelete() {
    imageEditorView.deleteElement(currentSelection);
  }

  @Override
  public void onCancel() {
    if (modeSnapshot != null) {
      EditorModel model = ParcelUtil.deserialize(modeSnapshot, EditorModel.CREATOR);
      imageEditorView.doneTextEditing();
      imageEditorView.setModel(model);
      currentSelection = null;
    }
  }

  @Override
  public void onCropReset() {
    for (int i = 0; undoAvailable && i < MAX_CROP_RESET_STEPS; i++) {
      imageEditorView.getModel().undo();
    }
  }

  @Override
  public void onFlipHorizontal() {
    imageEditorView.getModel().flipHorizontal();
  }

  @Override
  public void onRotate90AntiClockwise() {
    imageEditorView.getModel().rotate90anticlockwise();
  }

  @Override
  public void onCropAspectLock(boolean locked) {
    imageEditorView.getModel().setCropAspectLock(locked);
  }

  @Override
  public boolean isCropAspectLocked() {
    return imageEditorView.getModel().isCropAspectLocked();
  }

  @Override
  public void onRequestFullScreen(boolean fullScreen) {
    controller.onRequestFullScreen(fullScreen);
  }

  private void onUndoRedoAvailabilityChanged(boolean undoAvailable, boolean redoAvailable) {
    this.undoAvailable = undoAvailable;
    imageEditorHud.setUndoAvailability(undoAvailable);
    imageEditorHud.setRedoAvailability(redoAvailable);
  }

   private final ImageEditorView.TapListener selectionListener = new ImageEditorView.TapListener() {

     @Override
     public void onEntityDown(@Nullable EditorElement editorElement) {
       if (editorElement != null) {
         controller.onTouchEventsNeeded(true);
       } else {
         currentSelection = null;
         controller.onTouchEventsNeeded(false);
         enterHudMode(ImageEditorHud.Mode.NONE);
         imageEditorView.doneTextEditing();
       }
     }

     @Override
     public void onEntitySingleTap(@Nullable EditorElement editorElement) {
       currentSelection = editorElement;
       if (currentSelection != null) {
         if (editorElement.getRenderer() instanceof TextRenderer) {
           setTextElement(editorElement, (ColorableRenderer) editorElement.getRenderer(), imageEditorView.isTextEditing());
         } else {
           enterHudMode(ImageEditorHud.Mode.MOVE_DELETE);
         }
       }
     }

     @Override
      public void onEntityDoubleTap(@NonNull EditorElement editorElement) {
        currentSelection = editorElement;
        if (editorElement.getRenderer() instanceof TextRenderer) {
          setTextElement(editorElement, (ColorableRenderer) editorElement.getRenderer(), true);
        }
      }

     private void setTextElement(@NonNull EditorElement editorElement,
                                 @NonNull ColorableRenderer colorableRenderer,
                                 boolean startEditing)
     {
       int color = colorableRenderer.getColor();
       enterHudMode(ImageEditorHud.Mode.TEXT);
       imageEditorHud.setActiveColor(color);
       if (startEditing) {
         startTextEntityEditing(editorElement, false);
       }
     }
   };

  public interface Controller {
    void onTouchEventsNeeded(boolean needed);

    void onRequestFullScreen(boolean fullScreen);
  }
}
