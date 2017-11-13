package sequencer;

import java.awt.Rectangle;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import processing.core.PApplet;
import processing.core.PVector;
import processing.event.MouseEvent;
import sequencer.SequencerMain.InputState;
import sequencer.SequencerMain.PlayStatus;
import sequencer.SequencerMain.SeqButton;

public class SequencerBar
{
    private PApplet _p;
    private PVector _insets;
    private float _width;
    private float _height;
    private PVector _corner;
    private float _buttonHeight;
    private float _buttonWidth;
    private int _inactiveColor;
    private int _activeColor;
    private int _beatColor;
    private float _controlsWidth;
    private MuteButton _muteButton;
    
    public enum MidiInstrumentType
    {
        UNKNOWN, SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS, MULTIPLE_CHANNEL_MULTIPLE_INSTRUMENTS
    }

    private static final int INACTIVE_SYMBOL = -1; //no note/instrument
    private static final int MAX_EVENTS_AT_SAME_TIME = 10; //maximum amount of parallel midi events memorized by this object

    private int _steps;
    private int _stepsPerBeat;
    private int[][] _activeSteps;
    private MidiDevice _midiDevice;
    private int _activeSubTrack;
    private int _activeNote;
    private MidiInstrumentType _trackMidiInstrumentType;
    private int _activeChannel;
    private int _currentStep;
    private int _currentMaxSteps;
    private InputState _inputState;

    public SequencerBar(PVector corner, PVector insets, float areaWidth, float areaHeight, int steps, int numTracks, int stepsPerBeat, SequencerMain processingApp)
    {
        _p = processingApp;
        _insets = insets;
        _width = areaWidth;
        _height = areaHeight;
        _corner = corner;
        _buttonHeight = (_height/numTracks)  - 2 * _insets.y;
        _controlsWidth = 60;
        _buttonWidth = (_width - 2 * _insets.x - _controlsWidth )/steps;
        _inactiveColor = 255;
        _activeColor = 32;
        _beatColor = 128;
        _muteButton = new MuteButton(processingApp, processingApp, new Rectangle((int)(_corner.x + insets.x), (int)(_corner.y + insets.y), 40, 40), null, null);
        _inputState = processingApp.getInputState();

        _steps = steps;
        _stepsPerBeat = stepsPerBeat;
        _activeSubTrack = 0;
        _activeNote = 36;
        _activeChannel = 0;
        _currentStep = 0;
        _currentMaxSteps = steps;
        _activeSteps = new int[MAX_EVENTS_AT_SAME_TIME][steps]; 
        for(int curNC = 0; curNC < MAX_EVENTS_AT_SAME_TIME; curNC++)
        {
            for (int stepIdx = 0; stepIdx < _activeSteps[curNC].length; stepIdx++)
            {
                _activeSteps[curNC][stepIdx] = INACTIVE_SYMBOL; //no note/instrument
            }
        }
        _trackMidiInstrumentType = MidiInstrumentType.UNKNOWN;
    }

    public float getHeight()
    {
        return _buttonHeight + 2 * _insets.y;
    }

    public Rectangle getDimensions()
    {
        return new Rectangle((int)_corner.x, (int)_corner.y, (int)_width, (int)getHeight());
    }

    public void draw(int beat)
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
        _muteButton.draw();
    }

    public void mousePressed(MouseEvent event, InputState inputState) 
    {
        _muteButton.mousePressed(event, inputState);
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
                draw(0);
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
            _activeSteps[_activeSubTrack][activatedButton] = _activeNote;
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
        if((_activeSteps[_activeSubTrack][_currentStep] != INACTIVE_SYMBOL) && _muteButton.isNotSet())
        {
            ShortMessage midiMsg = new ShortMessage();
            try
            {
                switch (_trackMidiInstrumentType)
                {
                    case SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS:
                        midiMsg.setMessage(ShortMessage.NOTE_ON, 0, _activeNote, 127);
                        break;
                    case MULTIPLE_CHANNEL_MULTIPLE_INSTRUMENTS:
                        midiMsg.setMessage(ShortMessage.NOTE_ON, _activeChannel, _activeNote, 127);
                        break;
                    default:
                        return;
                }
                Receiver receiver = _midiDevice.getReceiver();
                receiver.send(midiMsg, -1);
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
    }

    public void setMidiDevice(MidiDevice midiDevice)
    {
        _midiDevice = midiDevice;
    }

    public void setActiveNote(int note)
    {
        _trackMidiInstrumentType = MidiInstrumentType.SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS;
        _activeNote = note;
    }

    public void setActiveChannel(int channel)
    {
        _trackMidiInstrumentType = MidiInstrumentType.MULTIPLE_CHANNEL_MULTIPLE_INSTRUMENTS;
        _activeChannel = channel;
    }
    
    public void setCurrentMaxSteps(int maxSteps)
    {
        _currentMaxSteps = maxSteps;
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
}
