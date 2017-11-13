package sequencer;

import java.awt.Rectangle;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;
import processing.event.MouseEvent;

public class SequencerMain extends PApplet
{
    private static final int STEPS_PER_BEAT = 4;
    private static final int STEPS = 32;
    private static final int NUM_TRACKS = 8;
    
    private long _stepTimer;
    private long _drawTimer;
    private long _passedTime;
    private int _beatsPerMinute;
    private int _currentStep;
    private PFont _f;
    private int _oldTime;
    private int _millisToPass;
    private MidiDevice _midiOut1;
    private MidiDevice _midiOut2;
    private SequencerBarArea _sequencerBarsArea;
    private PlayButton _playButton;
    private PlayStatus _playStatus;
    private StopButton _stopButton;
    private InputState _inputState;
    private StepLengthSelectButton _stepLengthSelectButton;

    public static void main(String[] args)
    {
        PApplet.main("sequencer.SequencerMain");
    }
    
    @Override
    public void settings()
    {
        size(1366, 768);
//        fullScreen();
    }

    @Override
    public void setup()
    {
        _stepTimer = 0;
        _passedTime = 0;
        _beatsPerMinute = 140;
        _oldTime = 0;
        _currentStep = 0;
        _millisToPass = 60000 / (_beatsPerMinute * STEPS_PER_BEAT);

        _f = createFont("Arial", 48, true);

        _inputState = new InputState();
        _inputState.setState(InputStateType.REGULAR);
        
        _playStatus = new PlayStatus(PlayStatus.STOPPED);
        _sequencerBarsArea = new SequencerBarArea(this, new Rectangle(100, 10, width - 120, height - 100));
        _playButton = new PlayButton(this, new Rectangle(width/2, height - 90, 80, 50), _playStatus, _inputState);
        _playButton.draw();
        _stopButton = new StopButton(this, new Rectangle(width/2 - 90, height - 90, 80, 50), _playStatus, _inputState);
        _stopButton.draw();
        _stepLengthSelectButton = new StepLengthSelectButton(this, new Rectangle(width/2 + 90, height - 90, 80, 50), _playStatus, _inputState);
        mapMidi(_sequencerBarsArea);
    }

    private void mapMidi(SequencerBarArea sequencerBarsArea)
    {
        MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
        for (Info curDevice : deviceInfos)
        {
            System.out.print("Name: " + curDevice.getName());
            System.out.print("  Description: " + curDevice.getDescription());
//            homeMapping(sequencerBarsArea, curDevice);
            windowsMapping(sequencerBarsArea, curDevice);
            System.out.println();
        }
    }
 
    private void windowsMapping(SequencerBarArea sequencerBarsArea, Info curDevice)
    {
        if(curDevice.getName().equals("Gervill") && curDevice.getDescription().equals("Software MIDI Synthesizer"))
        {
            try
            {
                _midiOut1 = MidiSystem.getMidiDevice(curDevice);
                _midiOut1.open();
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 0);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 1);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 2);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 3);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 4);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 5);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 6);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 7);
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
            System.out.print(" <---- SELECTED");
        }
        sequencerBarsArea.setBarsChannelAndNote(0, 10, 36);
        sequencerBarsArea.setBarsChannelAndNote(1, 10, 38);
        sequencerBarsArea.setBarsChannelAndNote(2, 10, 39);
        sequencerBarsArea.setBarsChannelAndNote(3, 10, 42);
        sequencerBarsArea.setBarsChannelAndNote(4, 10, 43);
        sequencerBarsArea.setBarsChannelAndNote(5, 10, 46);
        sequencerBarsArea.setBarsChannelAndNote(6, 10, 50);
        sequencerBarsArea.setBarsChannelAndNote(7, 10, 75);
    }

    private void homeMapping(SequencerBarArea sequencerBarsArea, Info curDevice)
    {
        if(curDevice.getName().equals("USB Midi 4i4o") && curDevice.getDescription().equals("External MIDI Port"))
        {
            try
            {
                _midiOut1 = MidiSystem.getMidiDevice(curDevice);
                _midiOut1.open();

                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 0);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 1);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 2);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 3);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 4);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 5);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 6);
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 7);
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
            System.out.print(" <---- SELECTED");
        }
        if(curDevice.getName().equals("MIDIOUT2 (USB Midi 4i4o)") && curDevice.getDescription().equals("External MIDI Port"))
        {
            try
            {
                _midiOut2 = MidiSystem.getMidiDevice(curDevice);
                _midiOut2.open();
                sequencerBarsArea.setMidiDeviceBar(_midiOut1, 7);
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
            System.out.print(" <---- SELECTED");
        }
        sequencerBarsArea.setBarsChannelAndNote(0, 1, 36);
        sequencerBarsArea.setBarsChannelAndNote(1, 1, 38);
        sequencerBarsArea.setBarsChannelAndNote(2, 1, 39);
        sequencerBarsArea.setBarsChannelAndNote(3, 1, 42);
        sequencerBarsArea.setBarsChannelAndNote(4, 1, 43);
        sequencerBarsArea.setBarsChannelAndNote(5, 1, 46);
        sequencerBarsArea.setBarsChannelAndNote(6, 1, 50);
        sequencerBarsArea.setBarsChannelAndNote(7, 1, 75);
    }

    @Override
    public void draw()
    {
        _passedTime = millis() - _oldTime;
        _oldTime = millis();
        if(_drawTimer <= 0)
        {
            _drawTimer = 100;
            _playButton.draw();
            _stopButton.draw();
            _stepLengthSelectButton.draw();
        }
        else
        {
            _drawTimer = _drawTimer - _passedTime;
        }
        if(_stepTimer <= 0)
        {
            _stepTimer = _millisToPass;
            if(_playStatus.isStopped())
            {
                _currentStep = 0;
                _sequencerBarsArea.sendStopped();
            }
            if(_playStatus.isPlaying())
            {
                _sequencerBarsArea.sendAdvance(_currentStep);
                _currentStep = _currentStep + 1;
                if(_currentStep >= STEPS)
                {
                    _currentStep = 0;
                }
            }
            background(255);
            _playButton.draw();
            _stopButton.draw();
            _stepLengthSelectButton.draw();
            _sequencerBarsArea.draw(_currentStep);
            showCurrentStepAsNumber();
        }
        else
        {
            _stepTimer = _stepTimer - _passedTime;
        }
    }
    
    private void showCurrentStepAsNumber()
    {
        textFont(_f);
        textAlign(LEFT);
        int previousColor = getGraphics().fillColor;
        fill(0);
        text(_currentStep, width/2, height/2);
        fill(previousColor);
    }

    @Override
    public void mousePressed(MouseEvent event)
    {
        _sequencerBarsArea.mousePressed(event, _inputState);
        _playButton.mousePressed(event, _inputState);
        _stopButton.mousePressed(event, _inputState);
        _stepLengthSelectButton.mousePressed(event, _inputState);
    }
    
    public class InputState
    {
        private InputStateType _state;

        public void setState(InputStateType newState)
        {
            _state = newState;
        }

        
        public void stepLengthSelectPressed()
        {
            switch (_state)
            {
                case STEP_LENGTH_SELECT_ENABLED:
                    _state = InputStateType.REGULAR;
                    break;
                case REGULAR:
                    _state = InputStateType.STEP_LENGTH_SELECT_ENABLED;
                default:
                    break;
            }
            System.out.println("new state: " + _state);
        }

        public void maxStepsSet()
        {
            _state = InputStateType.REGULAR;
        }

        public InputStateType getState()
        {
            return _state;
        }
    }
    
    public enum InputStateType
    {
        REGULAR, STEP_LENGTH_SELECT_ENABLED
    }

    public abstract class SeqButton
    {
        protected PApplet _mainApp;
        protected Rectangle _area;
        protected PlayStatus _myPlayStatus;
        protected InputState _myInputState;

        public SeqButton(PApplet mainApp, Rectangle area, PlayStatus playStatus, InputState inputState)
        {
            _mainApp = mainApp;
            _area = area;
            _myPlayStatus = playStatus;
            _myInputState = inputState;
        }

        public void mousePressed(MouseEvent event, InputState inputState)
        {
            if(event.getX() > _area.x && event.getX() < _area.x + _area.width && event.getY() > _area.y && event.getY() < _area.y + _area.height)
            {
                buttonPressed(inputState);
            }
        }

        protected abstract void buttonPressed(InputState inputState);

        public void draw()
        {
            int prevCol = _mainApp.getGraphics().fillColor;
            boolean prevStroke = _mainApp.getGraphics().stroke;
            int prevStrokeCol = _mainApp.getGraphics().strokeColor;
            _mainApp.noStroke();

            setColor();
            _mainApp.rect(_area.x, _area.y, _area.width, _area.height, 10);
            _mainApp.fill(prevCol);
            if(prevStroke)
            {
                _mainApp.stroke(prevStrokeCol);
            }
            else
            {
                _mainApp.noStroke();
            }
        }

        protected abstract void setColor();
    }

    public class PlayButton extends SeqButton
    {
        public PlayButton(PApplet mainApp, Rectangle area, PlayStatus playStatus, InputState inputState)
        {
            super(mainApp, area, playStatus, inputState);
        }


        protected void buttonPressed(InputState inputState)
        {
            if(_myPlayStatus.isPlaying())
            {
                _myPlayStatus.set(PlayStatus.PAUSED);
            }
            else if(_myPlayStatus.isPaused() || _myPlayStatus.isStopped())
            {
                _myPlayStatus.set(PlayStatus.PLAYING);
            }
        }

        protected void setColor()
        {
            if(_myPlayStatus.isPlaying())
            {
                _mainApp.fill(32, 128, 64);
            }
            else if(_myPlayStatus.isPaused())
            {
                _mainApp.fill(16, 64, 32);
            }
            else if(_myPlayStatus.isStopped())
            {
                _mainApp.fill(8, 32, 16);
            }
        }
    }
    
    public class StopButton extends SeqButton
    {
        public StopButton(SequencerMain sequencerMain, Rectangle rectangle, PlayStatus playStatus, InputState inputState)
        {
            super(sequencerMain, rectangle, playStatus, inputState);
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            if(!_myPlayStatus.isStopped())
            {
                _myPlayStatus.set(PlayStatus.STOPPED);
            }
        }

        @Override
        protected void setColor()
        {
            _mainApp.fill(32, 32, 32);
        }
    }

    public class StepLengthSelectButton extends SeqButton
    {
        public StepLengthSelectButton(PApplet mainApp, Rectangle area, PlayStatus playStatus, InputState inputState)
        {
            super(mainApp, area, playStatus, inputState);
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            inputState.stepLengthSelectPressed();
        }

        @Override
        protected void setColor()
        {
            switch (_myInputState._state)
            {
                case REGULAR:
                    _mainApp.fill(128,0,128);
                    break;
                case STEP_LENGTH_SELECT_ENABLED:
                    _mainApp.fill(192,0,192);
                    break;
            }
        }
    }

    public class PlayStatus
    {
        public static final int STOPPED = 0;
        private static final int PLAYING = 1;
        private static final int PAUSED = 2;

        private int _status;
        
        public PlayStatus(int status)
        {
            _status = status;
        }

        public boolean isPaused()
        {
            return _status == PAUSED;
        }

        public boolean isStopped()
        {
            return _status == STOPPED;
        }

        public int getStatus()
        {
            return _status;
        }

        public boolean isPlaying()
        {
            return _status == PLAYING;
        }

        public void set(int status)
        {
            _status = status;
        }
    }

    public class SequencerBarArea
    {
        private SequencerBar[] _sequencerBars;

        public SequencerBarArea(SequencerMain mainApp, Rectangle area)
        {
            PVector insets = new PVector(10, 5);
            _sequencerBars = new SequencerBar[NUM_TRACKS];
            _sequencerBars[0] = new SequencerBar(new PVector(area.x, area.y), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[0].draw(0);
            _sequencerBars[1] = new SequencerBar(new PVector(area.x, _sequencerBars[0].getDimensions().y + _sequencerBars[0].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[1].draw(0);
            _sequencerBars[2] = new SequencerBar(new PVector(area.x, _sequencerBars[1].getDimensions().y + _sequencerBars[1].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[2].draw(0);
            _sequencerBars[3] = new SequencerBar(new PVector(area.x, _sequencerBars[2].getDimensions().y + _sequencerBars[2].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[3].draw(0);
            _sequencerBars[4] = new SequencerBar(new PVector(area.x, _sequencerBars[3].getDimensions().y + _sequencerBars[3].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[4].draw(0);
            _sequencerBars[5] = new SequencerBar(new PVector(area.x, _sequencerBars[4].getDimensions().y + _sequencerBars[4].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[5].draw(0);
            _sequencerBars[6] = new SequencerBar(new PVector(area.x, _sequencerBars[5].getDimensions().y + _sequencerBars[5].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[6].draw(0);
            _sequencerBars[7] = new SequencerBar(new PVector(area.x, _sequencerBars[6].getDimensions().y + _sequencerBars[6].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[7].draw(0);
        }

        public void setBarsChannelAndNote(int barNr, int channel, int note)
        {
            _sequencerBars[barNr].setActiveChannelAndNote(channel, note);
        }

        public void setMidiDeviceBar(MidiDevice midiDevice, int barIdx)
        {
            _sequencerBars[barIdx].setMidiDevice(midiDevice);
        }

        public void sendAdvance(int currentStep)
        {
            for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
            {
                _sequencerBars[trackCnt].sendAdvance(currentStep);
            }
        }
        
        public void sendStopped()
        {
            for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
            {
                _sequencerBars[trackCnt].sendStopped();
            }
        }

        public void draw(int currentStep)
        {
            for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
            {
                _sequencerBars[trackCnt].draw(currentStep);
            }
        }

        public void mousePressed(MouseEvent event, InputState inputState)
        {
            for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
            {
                _sequencerBars[trackCnt].mousePressed(event, inputState);
            }
        }
    }

    public InputState getInputState()
    {
        return _inputState;
    }
}
