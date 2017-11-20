package sequencer;

import java.awt.Rectangle;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import processing.core.PApplet;
import processing.core.PVector;
import processing.event.MouseEvent;
import sequencer.SequencerMain.DrawType;
import sequencer.SequencerMain.InputState;
import sequencer.SequencerMain.PlayStatus;
import sequencer.SequencerMain.ScreenElement;
import sequencer.SequencerMain.SeqButton;
import sequencer.SequencerMain.TrackModel;

public class SequencerBar implements ScreenElement
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
    
    public enum MidiInstrumentType
    {
        /**
         * currently this differentiation doesn't make sense since everything is handled equally. 
         * for future releases it means that either a connected midi device provides different instruments
         * on the same channel by different notes(SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS), it provides different
         * instruments by addressing different channels (MULTIPLE_CHANNEL_MULTIPLE_INSTRUMENTS) or it provides 
         * one instrument on on channel (doesn't exist yet).  
         */
        
        UNKNOWN, SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS, MULTIPLE_CHANNEL_MULTIPLE_INSTRUMENTS
    }

    private static final int INACTIVE_SYMBOL = -1; //no note/instrument
    private static final int MAX_EVENTS_AT_SAME_TIME = 10; //maximum amount of parallel midi events memorized by this object

    private TrackModel _trackModel;
    private int _steps;
    private int _stepsPerBeat;
    private int[][] _activeSteps;
    private int _activeSubTrack;
    private MidiInstrumentType _trackMidiInstrumentType;
    private int _currentStep;
    private int _currentMaxSteps;

    public SequencerBar(PVector corner, PVector insets, int areaWidth, int trackHeight, SequencerMain.TrackModel trackModel, int cnt, SequencerMain processingApp)
    {
        _p = processingApp;
        _insets = insets;
        _width = areaWidth;
        _corner = corner;
        _buttonHeight = trackHeight  - 2 * _insets.y;
        _controlsWidth = 60;
        _buttonWidth = (_width - 2 * _insets.x - _controlsWidth )/trackModel.getNumberOfSteps();
        _inactiveColor = 255;
        _activeColor = 32;
        _beatColor = 128;
        int ctrlButtonHeight = (int)((_buttonHeight * 3)/8);
        _muteButton = new MuteButton(processingApp, processingApp, new Rectangle((int)(_corner.x + insets.x), (int)(_corner.y + insets.y), 40, ctrlButtonHeight), null, null);
        _instrumentSelectButton = new InstrumentSelectButton(processingApp, processingApp, new Rectangle((int)(_corner.x + insets.x), (int)(_corner.y + insets.y) + 40, 40, ctrlButtonHeight), null, null);
        _inputState = processingApp.getInputState();

        _trackModel = trackModel;
        _steps = trackModel.getNumberOfSteps();
        _stepsPerBeat = trackModel.getStepsPerBeat();
        _activeSubTrack = 0;
        _currentStep = 0;
        _currentMaxSteps = _steps;
        _activeSteps = new int[MAX_EVENTS_AT_SAME_TIME][_steps]; 
        for(int curNC = 0; curNC < MAX_EVENTS_AT_SAME_TIME; curNC++)
        {
            for (int stepIdx = 0; stepIdx < _activeSteps[curNC].length; stepIdx++)
            {
                _activeSteps[curNC][stepIdx] = INACTIVE_SYMBOL; //no note/instrument
            }
        }
        _trackMidiInstrumentType = trackModel.getInstrumentType();
    }

    public float getHeight()
    {
        return _buttonHeight + 2 * _insets.y;
    }

    public Rectangle getDimensions()
    {
        return new Rectangle((int)_corner.x, (int)_corner.y, (int)_width, (int)getHeight());
    }

    @Override
    public void draw(DrawType type)
    {
        int prevCol = _p.getGraphics().fillColor;
        _p.fill(_inactiveColor);
        for(int stepIdx = 0; stepIdx < _steps; stepIdx++)
        {
            if(stepIdx != _currentStep)
            {
                if(_activeSteps[_activeSubTrack][stepIdx] != INACTIVE_SYMBOL)
                {
                    _p.fill(0, 0, 255);
                }
                else 
                {
                    if (stepIdx % _stepsPerBeat == 0)
                    {
                        _p.fill(_beatColor);
                        if(stepIdx == _currentMaxSteps)
                        {
                            _p.fill(128, 0, 255);
                        }
                    }
                    else
                    {
                        _p.fill(_inactiveColor);
                        if(stepIdx == _currentMaxSteps)
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
                        activateNote(activatedButton);
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
        _currentMaxSteps = activatedButton + 1;
        inputState.maxStepsSet();
        System.out.println("max steps " + _currentMaxSteps);
    }

    private void activateNote(int activatedButton)
    {
        if(_activeSteps[_activeSubTrack][activatedButton] == INACTIVE_SYMBOL)
        {
            _activeSteps[_activeSubTrack][activatedButton] = _trackModel.getNote();
        }
        else
        {
            _activeSteps[_activeSubTrack][activatedButton] = INACTIVE_SYMBOL;
        }
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

    public void sendAdvance(int currentStep)
    {
        ShortMessage noteOffMsg = new ShortMessage();
        try
        {
            noteOffMsg.setMessage(ShortMessage.NOTE_OFF, _trackModel.getChannel(), _trackModel.getNote(), 0);
            _trackModel.getMidiDevice().getReceiver().send(noteOffMsg, -1);
        }
        catch (MidiUnavailableException exc1)
        {
            exc1.printStackTrace();
        }
        catch (InvalidMidiDataException exc)
        {
            exc.printStackTrace();
        }
        if((_activeSteps[_activeSubTrack][_currentStep] != INACTIVE_SYMBOL) && _muteButton.isNotSet())
        {
            try
            {
                ShortMessage midiMsg = new ShortMessage();
                switch (_trackMidiInstrumentType)
                {
                    case SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS:
                        midiMsg.setMessage(ShortMessage.NOTE_ON, _trackModel.getChannel(), _trackModel.getNote(), 120);
                        _trackModel.getMidiDevice().getReceiver().send(midiMsg, -1);
                        break;
                    case MULTIPLE_CHANNEL_MULTIPLE_INSTRUMENTS:
                        midiMsg.setMessage(ShortMessage.NOTE_ON, _trackModel.getChannel(), _trackModel.getNote(), 120);
                        _trackModel.getMidiDevice().getReceiver().send(midiMsg, -1);
                        break;
                    default:
                        break;
                }
            }
            catch (InvalidMidiDataException exc)
            {
                exc.printStackTrace();
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
        }
        _currentStep++;
        if(_currentStep >= _currentMaxSteps)
        {
            _currentStep = 0;
        }
    }
    
    public void sendStopped()
    {
        _currentStep = 0;
        try
        {
            sendNoteOff();
            sendNoteOff();
        }
        catch (InvalidMidiDataException exc)
        {
            exc.printStackTrace();
        }
        catch (MidiUnavailableException exc)
        {
            exc.printStackTrace();
        }
    }

    private void sendNoteOff() throws InvalidMidiDataException, MidiUnavailableException
    {
        ShortMessage offMsg = new ShortMessage();
        offMsg.setMessage(ShortMessage.NOTE_ON, _trackModel.getChannel(), _trackModel.getNote(), 0);
        _trackModel.getMidiDevice().getReceiver().send(offMsg, -1);
    }

    public void setCurrentMaxSteps(int maxSteps)
    {
        _currentMaxSteps = maxSteps;
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
        private boolean _isMuted;

        public MuteButton(SequencerMain sequencerMain, PApplet mainApp, Rectangle area, PlayStatus playStatus, InputState inputState)
        {
            sequencerMain.super(mainApp, area, playStatus, inputState);
            _isMuted = false;
        }

        public boolean isNotSet()
        {
            return !_isMuted;
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _isMuted = !_isMuted;
        }

        @Override
        protected void setColor()
        {
            if(_isMuted)
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
