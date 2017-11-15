package sequencer;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;
import processing.event.MouseEvent;
import sequencer.SequencerMain.PlayStatusType;

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
    private InputState _inputState;
    private Screen _currentScreen;
    private PlayStatus _playStatus;
    private Map<String, Screen> _screens;
    
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

        _playStatus = new PlayStatus(PlayStatusType.STOPPED);

        _inputState = new InputState();
        _inputState.setState(InputStateType.REGULAR);

        _screens = new HashMap<>();
        TracksScreen tracksScreen = new TracksScreen(this);
        tracksScreen.create();
        _sequencerBarsArea = tracksScreen.getSequencerArea();
        InstrumentSelectScreen  instrumentSelectScreen = new InstrumentSelectScreen(this);
        _screens.put("trackScreen", tracksScreen);
        _screens.put("instrumentSelect", instrumentSelectScreen);
        
        _currentScreen = tracksScreen;
        _currentScreen.draw(DrawType.NO_STEP_ADVANCE);
        
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
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 0);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 1);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 2);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 3);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 4);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 5);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 6);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 7);
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

                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 0);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 1);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 2);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 3);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 4);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 5);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 6);
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 7);
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
                sequencerBarsArea.setMidiDeviceForTrack(_midiOut1, 7);
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
            System.out.print(" <---- SELECTED");
        }
        sequencerBarsArea.setBarsChannelAndNote(0, 0, 36);
        sequencerBarsArea.setBarsChannelAndNote(1, 0, 38);
        sequencerBarsArea.setBarsChannelAndNote(2, 0, 39);
        sequencerBarsArea.setBarsChannelAndNote(3, 0, 42);
        sequencerBarsArea.setBarsChannelAndNote(4, 0, 43);
        sequencerBarsArea.setBarsChannelAndNote(5, 0, 46);
        sequencerBarsArea.setBarsChannelAndNote(6, 0, 50);
        sequencerBarsArea.setBarsChannelAndNote(7, 0, 75);
    }

    @Override
    public void draw()
    {
        _passedTime = millis() - _oldTime;
        _oldTime = millis();
        if(_drawTimer <= 0)
        {
            _drawTimer = 100; //redraw every 10 microseconds
            _currentScreen.draw(DrawType.NO_STEP_ADVANCE);
        }
        else
        {
            _drawTimer = _drawTimer - _passedTime;
        }
        if(_stepTimer <= 0)
        {
            _stepTimer = _millisToPass; //redraw according to beats per minute
            switch (_playStatus.getStatus())
            {
                case STOPPED:
                    _currentStep = 0;
                    _sequencerBarsArea.sendStopped();
                    break;
                case PLAYING:
                    _sequencerBarsArea.sendAdvance(_currentStep);
                    _currentStep = _currentStep + 1;
                    if(_currentStep >= STEPS)
                    {
                        _currentStep = 0;
                    }
                    break;
                case PAUSED:
                    //do nothing when paused
                    break;
                default:
                    break;
            }
            _currentScreen.draw(DrawType.STEP_ADVANCE);
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
        if(_inputState.getState() == InputStateType.INSTRUMENT_SELECT_ACTIVE)
        {
            _inputState.setState(InputStateType.REGULAR);
            return;
        }
        _currentScreen.mousePressed(event, _inputState);
    }

    public InputState getInputState()
    {
        return _inputState;
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
        }

        public void maxStepsSet()
        {
            _state = InputStateType.REGULAR;
        }

        public InputStateType getState()
        {
            return _state;
        }

        public void selectInstrumentPressed()
        {
            System.out.println("instrument select pressed");
            if(_state == InputStateType.REGULAR)
            {
                _state = InputStateType.INSTRUMENT_SELECT_ACTIVE;
            }
        }
    }
    
    public enum InputStateType
    {
        REGULAR, STEP_LENGTH_SELECT_ENABLED, INSTRUMENT_SELECT_ACTIVE
    }
    
    public enum DrawType
    {
        NO_STEP_ADVANCE, STEP_ADVANCE
    }

    public abstract class SeqButton implements ScreenElement
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

        public void draw(DrawType type)
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
            switch (_myPlayStatus.getStatus())
            {
                case PLAYING:
                    _myPlayStatus.set(PlayStatusType.PAUSED);
                    break;
                case PAUSED:
                case STOPPED:
                    _myPlayStatus.set(PlayStatusType.PLAYING);
                default:
                    break;
            }
        }

        protected void setColor()
        {
            switch (_myPlayStatus.getStatus())
            {
                case PLAYING:
                    _mainApp.fill(32, 128, 64);
                    break;
                case PAUSED:
                    _mainApp.fill(16, 64, 32);
                    break;
                case STOPPED:
                    _mainApp.fill(8, 32, 16);
                    break;
                default:
                    break;
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
            if(_myPlayStatus.getStatus() != PlayStatusType.STOPPED)
            {
                _myPlayStatus.set(PlayStatusType.STOPPED);
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
                case INSTRUMENT_SELECT_ACTIVE:
                    _mainApp.fill(128,0,128);
                    break;
                case STEP_LENGTH_SELECT_ENABLED:
                    _mainApp.fill(192,0,192);
                    break;
            }
        }
    }

    public enum PlayStatusType
    {
        PAUSED, STOPPED, PLAYING
    }

    public class PlayStatus
    {
        private PlayStatusType _status;
        
        public PlayStatus(PlayStatusType status)
        {
            _status = status;
        }
            
        public PlayStatusType getStatus()
        {
            return _status;
        }

        public void set(PlayStatusType status)
        {
            _status = status;
        }
    }

    public class SequencerBarArea implements ScreenElement
    {
        private SequencerBar[] _sequencerBars;

        public SequencerBarArea(SequencerMain mainApp, Rectangle area)
        {
            PVector insets = new PVector(10, 5);
            _sequencerBars = new SequencerBar[NUM_TRACKS];
            _sequencerBars[0] = new SequencerBar(new PVector(area.x, area.y), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[0].draw(DrawType.NO_STEP_ADVANCE);
            _sequencerBars[1] = new SequencerBar(new PVector(area.x, _sequencerBars[0].getDimensions().y + _sequencerBars[0].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[1].draw(DrawType.NO_STEP_ADVANCE);
            _sequencerBars[2] = new SequencerBar(new PVector(area.x, _sequencerBars[1].getDimensions().y + _sequencerBars[1].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[2].draw(DrawType.NO_STEP_ADVANCE);
            _sequencerBars[3] = new SequencerBar(new PVector(area.x, _sequencerBars[2].getDimensions().y + _sequencerBars[2].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[3].draw(DrawType.NO_STEP_ADVANCE);
            _sequencerBars[4] = new SequencerBar(new PVector(area.x, _sequencerBars[3].getDimensions().y + _sequencerBars[3].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[4].draw(DrawType.NO_STEP_ADVANCE);
            _sequencerBars[5] = new SequencerBar(new PVector(area.x, _sequencerBars[4].getDimensions().y + _sequencerBars[4].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[5].draw(DrawType.NO_STEP_ADVANCE);
            _sequencerBars[6] = new SequencerBar(new PVector(area.x, _sequencerBars[5].getDimensions().y + _sequencerBars[5].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[6].draw(DrawType.NO_STEP_ADVANCE);
            _sequencerBars[7] = new SequencerBar(new PVector(area.x, _sequencerBars[6].getDimensions().y + _sequencerBars[6].getDimensions().height), insets, area.width, area.height, STEPS, NUM_TRACKS, STEPS_PER_BEAT, mainApp);
            _sequencerBars[7].draw(DrawType.NO_STEP_ADVANCE);
        }

        public void setBarsChannelAndNote(int trackNr, int channel, int note)
        {
            _sequencerBars[trackNr].setActiveChannelAndNote(channel, note);
        }

        public void setMidiDeviceForTrack(MidiDevice midiDevice, int trackIdx)
        {
            _sequencerBars[trackIdx].setMidiDevice(midiDevice);
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

        @Override
        public void draw(DrawType type)
        {
            if(type == DrawType.STEP_ADVANCE)
            {
                for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
                {
                    _sequencerBars[trackCnt].draw(type);
                }
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

    public class TracksScreen implements Screen
    {
        private List<ScreenElement> _elements;
        private SequencerMain _parent;
        private SequencerBarArea _sequencerBarsArea;
        
        public TracksScreen(SequencerMain parent)
        {
            _elements = new ArrayList<>();
            _parent = parent;
        }

        public SequencerBarArea getSequencerArea()
        {
            
            return this._sequencerBarsArea;
        }

        @Override
        public void add(ScreenElement element)
        {
            _elements.add(element);
        }

        @Override
        public void create()
        {
            SequencerBarArea sequencerBarsArea = new SequencerBarArea(_parent, new Rectangle(100, 10, width - 120, height - 100));
            this._sequencerBarsArea = sequencerBarsArea;
            PlayButton playButton = new PlayButton(_parent, new Rectangle(width/2, height - 90, 80, 50), _playStatus, _inputState);
            StopButton stopButton = new StopButton(_parent, new Rectangle(width/2 - 90, height - 90, 80, 50), _playStatus, _inputState);
            StepLengthSelectButton stepLengthSelectButton = new StepLengthSelectButton(_parent, new Rectangle(width/2 + 90, height - 90, 80, 50), _playStatus, _inputState);
            
            add(sequencerBarsArea);
            add(playButton);
            add(stopButton);
            add(stepLengthSelectButton);
        }

        @Override
        public void draw(DrawType type)
        {
            for (ScreenElement curElem : _elements)
            {
                curElem.draw(type);
            }
        }

        @Override
        public void mousePressed(MouseEvent event, InputState inputState)
        {
            for (ScreenElement curElem : _elements)
            {
                curElem.mousePressed(event, inputState);
            }
        }
    }
    
    public class InstrumentSelectScreen implements Screen
    {
        private List<Info> _devices;

        public InstrumentSelectScreen(SequencerMain sequencerMain)
        {
            MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
            _devices = new ArrayList<>();
            for (Info curDevice : deviceInfos)
            {
                System.out.print("Name: " + curDevice.getName());
                System.out.print("  Description: " + curDevice.getDescription());
                System.out.println();
                _devices.add(curDevice);
            }
        }

        @Override
        public void add(ScreenElement element)
        {
        }

        @Override
        public void mousePressed(MouseEvent event, InputState inputState)
        {
        }

        @Override
        public void draw(DrawType type)
        {
        }

        @Override
        public void create()
        {
        }
    }

    public interface Screen
    {
        void add(ScreenElement element);

        void mousePressed(MouseEvent event, InputState inputState);

        void draw(DrawType type);

        void create();
    }

    public interface ScreenElement
    {
        void draw(DrawType type);

        void mousePressed(MouseEvent event, InputState inputState);
    }
}
