package sequencer;

import java.awt.Rectangle;

import processing.core.PApplet;
import processing.core.PVector;
import processing.event.MouseEvent;
import sequencer.SequencerMain.DrawType;
import sequencer.SequencerMain.InputState;
import sequencer.SequencerMain.PlayStatus;
import sequencer.SequencerMain.ScreenElement;
import sequencer.SequencerMain.SeqButton;
import sequencer.SequencerMain.TrackModel;

public class StepSequencerBar implements ScreenElement
{
    private PApplet _p;
    private PVector _insets;
    private float _width;
    private PVector _corner;
    private float _buttonHeight;
    private float _buttonWidth;
    private int _inactiveColor;
    private int _activeColor;
    private int _beatColor;
    private float _controlsWidth;
    private MuteButton _muteButton;
    private InputState _inputState;
    private InstrumentSelectButton _instrumentSelectButton;
    
    private TrackModel _trackModel;
    private int _steps;

    public StepSequencerBar(Rectangle barArea, PVector insets, TrackModel trackModel, SequencerMain mainApp)
    {
        _trackModel = trackModel;
        _steps = trackModel.getNumberOfSteps();

        _p = mainApp;
        _insets = insets;
        _width = barArea.width;
        _corner = new PVector(barArea.x, barArea.y);
        _buttonHeight = barArea.height  - 2 * _insets.y;
        _controlsWidth = 60;
        _buttonWidth = (_width - 2 * _insets.x - _controlsWidth )/trackModel.getNumberOfSteps();
        _inactiveColor = 255;
        _activeColor = 32;
        _beatColor = 128;
        int ctrlButtonHeight = (int)((_buttonHeight * 3)/8);
        _muteButton = new MuteButton(mainApp, mainApp, new Rectangle((int)(_corner.x + insets.x), (int)(_corner.y + insets.y), 40, ctrlButtonHeight), null, null);
        _instrumentSelectButton = new InstrumentSelectButton(mainApp, mainApp, new Rectangle((int)(_corner.x + insets.x), (int)(_corner.y + insets.y) + 40, 40, ctrlButtonHeight), null, null);
        _inputState = mainApp.getInputState();
    }

    @Override
    public void draw(DrawType type)
    {
        int prevCol = _p.getGraphics().fillColor;
        _p.fill(_inactiveColor);
        for(int stepIdx = 0; stepIdx < _steps; stepIdx++)
        {
            if(!_trackModel.isCurrentStep(stepIdx))
            {
                if(_trackModel.isStepActive(stepIdx))
                {
                    _p.fill(0, 0, 255);
                }
                else 
                {
                    if(_trackModel.isFirstStepInBeat(stepIdx))
                    {
                        _p.fill(_beatColor);
                        if(_trackModel.isCurrentMaxStep(stepIdx))
                        {
                            _p.fill(128, 0, 255);
                        }
                    }
                    else
                    {
                        _p.fill(_inactiveColor);
                        if(_trackModel.isCurrentMaxStep(stepIdx))
                        {
                            _p.fill(128, 0, 255);
                        }
                    }
                }
            }
            else
            {
                _p.fill(_activeColor);
            }
            _p.rect(stepIdx * _buttonWidth + _insets.x + _corner.x + _controlsWidth, _insets.y + _corner.y, _buttonWidth, _buttonHeight);
        }
        _p.fill(prevCol);
        _muteButton.draw(type);
        _instrumentSelectButton.draw(type);
    }
    
    public void mousePressed(MouseEvent event, InputState inputState) 
    {
        _muteButton.mousePressed(event, inputState);
        _instrumentSelectButton.mousePressed(event, inputState);
        int mouseY = event.getY();
        float cornerY = _corner.y + _insets.y;
        if(mouseY > cornerY && mouseY < (cornerY + _buttonHeight))
        {
            int activatedButton = getClickedButtonIdx(event);
            if(activatedButton != -1)
            {
                switch (_inputState.getState())
                {
                    case REGULAR:
                        _trackModel.toggleActivationState(activatedButton);
                        break;
                    case STEP_LENGTH_SELECT_ENABLED:
                        setNewMaxSteps(inputState, activatedButton);
                        break;
                    default:
                        break;
                }
                draw(DrawType.NO_STEP_ADVANCE);
            }
        }
    }

    private void setNewMaxSteps(InputState inputState, int activatedButton)
    {
        _trackModel.setCurrentMaxSteps(activatedButton + 1);
        inputState.maxStepsSet();
    }

    private int getClickedButtonIdx(MouseEvent event)
    {
        int activatedButton = -1;
        float offset = _insets.x + _corner.x + _controlsWidth;
        for(int cnt = 0; cnt < _steps; cnt++)
        {
            int mouseX = event.getX();
            if(mouseX > cnt * _buttonWidth + offset)
            {
                if(mouseX < (cnt + 1) * _buttonWidth + offset)
                {
                   activatedButton = cnt;
                   break;
                }
            }
        }
        return activatedButton;
    }

    public class InstrumentSelectButton extends SeqButton
    {
        public InstrumentSelectButton(SequencerMain processingApp, SequencerMain processingApp2, Rectangle rectangle, PlayStatus playStatus, InputState inputState)
        {
            processingApp.super(processingApp2, rectangle, playStatus, inputState);
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _inputState.selectInstrumentPressed(_trackModel);
        }

        @Override
        protected void setColor()
        {
            _mainApp.fill(140, 0, 140);
        }
    }

    public class MuteButton extends SeqButton
    {
        public MuteButton(SequencerMain sequencerMain, PApplet mainApp, Rectangle area, PlayStatus playStatus, InputState inputState)
        {
            sequencerMain.super(mainApp, area, playStatus, inputState);
            _trackModel.setMuteStatus(false);
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _trackModel.setMuteStatus(!_trackModel.isMuted());
        }

        @Override
        protected void setColor()
        {
            if(_trackModel.isMuted())
            {
                _mainApp.fill(255, 0, 0);
            }
            else
            {
                _mainApp.fill(128);
            }
        }
    }

    public static float computHeight(int totalHeight, int numTracks, float y)
    {
        return (totalHeight/numTracks)  - 2 * y;
    }
}
