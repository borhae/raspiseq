package sequencer;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;
import processing.event.MouseEvent;

public class SequencerMain extends PApplet
{
    private static final int ARPEGGIATOR_DIVISION = 4;
    private static final String INSTRUMENT_SELECT_SCREEN_ID = "instrumentSelect";
    private static final String TRACK_SCREEN_ID = "trackScreen";
    private static final int STEPS_PER_BEAT = 4;
    private static final int STEPS = 32;
    private static final int NUM_TRACKS = 8;
    
    private long _stepTimer;
    private long _arpeggioTimer;
    private long _drawTimer;
    private long _passedTime;
    private int _beatsPerMinute;
    private int _currentStep;
    private PFont _counterFont;
    private PFont _instrumentSelectFont;
    private int _oldTime;
    private int _millisToPass;
    private InputState _inputState;
    private Screen _currentScreen;
    private PlayStatus _playStatus;
    private Map<String, Screen> _screens;
    private TracksModel _tracksModel;
    
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
        _arpeggioTimer = 0;
        _passedTime = 0;
        _beatsPerMinute = 160;
        _oldTime = 0;
        _currentStep = 0;
        _millisToPass = 60000 / (_beatsPerMinute * STEPS_PER_BEAT);
        System.out.println("millis per step: " + _millisToPass);
        System.out.println("millis per arp: " + _millisToPass / ARPEGGIATOR_DIVISION);

        _counterFont = createFont("Arial", 48, true);
        _instrumentSelectFont = createFont("Arial", 12, true);

        _playStatus = new PlayStatus(PlayStatusType.STOPPED);

        _inputState = new InputState();
        _inputState.setState(InputStateType.REGULAR);

        
        MidiDevice midiInDevice = null;
        try
        {
            midiInDevice = createMidiInDevice();
        }
        catch (MidiUnavailableException exc)
        {
            exc.printStackTrace();
            return;
        }
        
        _tracksModel = new TracksModel(NUM_TRACKS, STEPS, STEPS_PER_BEAT, _beatsPerMinute, midiInDevice); 
        _screens = new HashMap<>();
        TracksScreen tracksScreen = new TracksScreen(this, _tracksModel);
        tracksScreen.create();

        InstrumentSelectScreen  instrumentSelectScreen = new InstrumentSelectScreen(this, _inputState);
        instrumentSelectScreen.create();
        
        _screens.put(TRACK_SCREEN_ID, tracksScreen);
        _screens.put(INSTRUMENT_SELECT_SCREEN_ID, instrumentSelectScreen);
        
        _currentScreen = tracksScreen;
        _currentScreen.draw(DrawType.NO_STEP_ADVANCE);
    }

    public static String midiMessageToString(ShortMessage sMessage)
    {
        return "Channel: " + sMessage.getChannel() + ", Command: " + sMessage.getCommand() + ", Data1: " + sMessage.getData1() + ", Data2: " + sMessage.getData2() + ", Length: " + sMessage.getLength() + ", Status: " + sMessage.getStatus();
    }

    private MidiDevice createMidiInDevice() throws MidiUnavailableException
    {
        Info midiInDevice = null;
        Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
        for (Info curDevice : deviceInfos)
        {
            if(curDevice.getName().equals("Keystation Mini 32") && curDevice.getDescription().equals("No details available") )
            {
                midiInDevice = curDevice;
            }
        }
        return MidiSystem.getMidiDevice(midiInDevice);
    }

    private void mapMidi(List<TrackModel> tracksModels)
    {
            homeMapping(tracksModels);
//            windowsMapping(tracksModels);
            System.out.println();
    }
 
    private void windowsMapping(List<TrackModel> tracksModels)
    {
        MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
        int channel = 10;
        for (Info curDevice : deviceInfos)
        {
            System.out.print("Name: " + curDevice.getName());
            System.out.print("  Description: " + curDevice.getDescription());
            if(curDevice.getName().equals("Gervill") && curDevice.getDescription().equals("Software MIDI Synthesizer"))
            {
                for (TrackModel trackModel : tracksModels)
                {
                    trackModel.setDeviceInfo(curDevice);
                    trackModel.setChannel(channel);
                }
                System.out.print(" <---- SELECTED");
            }                                                                                                                           
            System.out.println();
            tracksModels.get(0).setNote(36);
            tracksModels.get(1).setNote(38);
            tracksModels.get(2).setNote(39);
            tracksModels.get(3).setNote(42);
            tracksModels.get(4).setNote(43);
            tracksModels.get(5).setNote(46);
            tracksModels.get(6).setNote(50);
            tracksModels.get(7).setNote(75);
        }
    }

    private void homeMapping(List<TrackModel> tracksModels)
    {
        MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
        int channel = 0;
        for (Info curDevice : deviceInfos)
        {
            System.out.print("Name: " + curDevice.getName());
            System.out.print("  Description: " + curDevice.getDescription());
            if(curDevice.getName().equals("USB Midi 4i4o") && curDevice.getDescription().equals("External MIDI Port"))
            {
                for (TrackModel trackModel : tracksModels)
                {
                    trackModel.setDeviceInfo(curDevice);
                    trackModel.setChannel(channel);
                }
                System.out.print(" <---- SELECTED");
            }
            System.out.println("");
            tracksModels.get(0).setNote(36);
            tracksModels.get(1).setNote(38);
            tracksModels.get(2).setNote(39);
            tracksModels.get(3).setNote(42);
            tracksModels.get(4).setNote(43);
            tracksModels.get(5).setNote(46);
            tracksModels.get(6).setNote(50);
            tracksModels.get(7).setNote(75);
        }
    }

    @Override
    public void draw()
    {
        _passedTime = millis() - _oldTime;
        _oldTime = millis();
        if(_drawTimer <= 0)
        {
            _drawTimer = 10; //redraw every 10 microseconds
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
                    _tracksModel.sendStopped();
                    _playStatus.hasStopped();
                    break;
                case PLAYING:
                    _tracksModel.sendPlaying();
                    _tracksModel.sendAdvance(_currentStep);
                    _currentStep = _currentStep + 1;
                    if(_currentStep >= STEPS)
                    {
                        _currentStep = 0;
                    }
                    break;
                case PAUSED:
                    _tracksModel.sendPaused();
                    break;
                case RECORDING:
                    _tracksModel.sendRecording();
                    break;
                default:
                    break;
            }
            background(255);
            _currentScreen.draw(DrawType.STEP_ADVANCE);
            showCurrentStepAsNumber();
        }
        else
        {
            _stepTimer = _stepTimer - _passedTime;
            if(_arpeggioTimer <= 0)
            {
                _arpeggioTimer = _millisToPass / ARPEGGIATOR_DIVISION;
                _tracksModel.arpeggiator();
            }
            else
            {
                _arpeggioTimer = _arpeggioTimer - _passedTime;
            }
        }
    }

    private void showCurrentStepAsNumber()
    {
        textFont(_counterFont);
        textAlign(LEFT);
        int previousColor = getGraphics().fillColor;
        fill(0);
        text(_currentStep, width/2, height/2);
        fill(previousColor);
    }

    @Override
    public void mousePressed(MouseEvent event)
    {
        _currentScreen.mousePressed(event, _inputState);
    }

    public InputState getInputState()
    {
        return _inputState;
    }

    public class InputState
    {
        private InputStateType _state;
        private TrackModel _intstrumentSelectingTrack;

        public InputState()
        {
        }
        
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

        public void selectInstrumentPressed(TrackModel trackModel)
        {
            if(_state == InputStateType.REGULAR)
            {
                _intstrumentSelectingTrack = trackModel;
                _state = InputStateType.INSTRUMENT_SELECT_ACTIVE;
                background(255);
                InstrumentSelectScreen instrumentSelectScreen = (InstrumentSelectScreen) _screens.get(INSTRUMENT_SELECT_SCREEN_ID);
                instrumentSelectScreen.setSelectingTrack(_intstrumentSelectingTrack);
                _currentScreen = instrumentSelectScreen;
                _currentScreen.draw(DrawType.NO_STEP_ADVANCE);
            }
        }

        public void instrumentSelected()
        {
            if(_state == InputStateType.INSTRUMENT_SELECT_ACTIVE)
            {
                _state = InputStateType.REGULAR;
                _intstrumentSelectingTrack.closeNoteSelector();
                _intstrumentSelectingTrack.rewriteNote();
                background(255);
                _currentScreen = _screens.get(TRACK_SCREEN_ID);
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
    
    public abstract class InstrumentSelectButton extends SeqButton
    {
        protected TrackModel _trackModel;

        public InstrumentSelectButton(PApplet mainApp, Rectangle area, PlayStatus playStatus, InputState inputState, TrackModel trackModel)
        {
            super(mainApp, area, playStatus, inputState);
            _trackModel = trackModel;
        }

        public void setActiveTrack(TrackModel instrumentSelectingTrack)
        {
            _trackModel = instrumentSelectingTrack;
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            
        }
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

        @Override
        public void draw(DrawType type)
        {
            super.draw(type);
            int triangleHeight = 30;
            int triangleWidth = 30;
            int upperleftX = _area.x + 30;
            int upperLeftY = _area.y + 10;
            _mainApp.line(upperleftX, upperLeftY, upperleftX + triangleWidth, upperLeftY + (triangleHeight/2));
            _mainApp.line(upperleftX + triangleWidth, upperLeftY + (triangleHeight/2), upperleftX, upperLeftY + triangleHeight);
            _mainApp.line(upperleftX, upperLeftY + triangleHeight, upperleftX, upperLeftY);
        }

        protected void setColor()
        {
            switch (_myPlayStatus.getStatus())
            {
                case PLAYING:
                    _mainApp.fill(32, 200, 64);
                    break;
                case PAUSED:
                    _mainApp.fill(16, 128, 32);
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
        PAUSED, STOPPED, PLAYING, RECORDING
    }

    public class PlayStatus
    {
        private PlayStatusType _status;
        
        public PlayStatus(PlayStatusType status)
        {
            _status = status;
        }
            
        public void hasStopped()
        {
            _status = PlayStatusType.STOPPED;
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
        private List<StepSequencerBar> _sequencerBars;

        public SequencerBarArea(SequencerMain mainApp, Rectangle area, TracksModel tracksModel)
        {
            PVector insets = new PVector(10, 5);
            _sequencerBars = new ArrayList<>();
            List<TrackModel> tracksModels = tracksModel.getTrackModels();
            int trackHeight = (int)StepSequencerBar.computHeight(area.height, tracksModels.size(), insets.y);
            int cnt = 0;
            for (TrackModel trackModel : tracksModels)
            {
                StepSequencerBar newTrack = null;
                Rectangle barArea = new Rectangle(area.x, area.y + cnt * trackHeight, area.width, trackHeight);
                if(!(trackModel instanceof NoteLooperModel))
                {
                    newTrack = 
                            new StepSequencerBar(barArea, insets, trackModel, mainApp);
                } 
                else
                {
                    newTrack = 
                            new NoteLooperBar(barArea, insets, trackModel, mainApp);
                }
                _sequencerBars.add(newTrack);
                cnt++;
            }
        }

        @Override
        public void draw(DrawType type)
        {
            if(type == DrawType.STEP_ADVANCE)
            {
                for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
                {
                    _sequencerBars.get(trackCnt).draw(type);
                }
            }
        }

        public void mousePressed(MouseEvent event, InputState inputState)
        {
            for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
            {
                _sequencerBars.get(trackCnt).mousePressed(event, inputState);
            }
        }
    }
    
    public class TrackModel
    {
        protected static final int INACTIVE_SYMBOL = -1; //no note/instrument
        protected static final int MAX_EVENTS_AT_SAME_TIME = 10; //maximum amount of parallel midi events memorized by this object
   
        protected int _numberOfSteps;
        protected int _stepsPerBeat;
        protected MidiDevice _midiDevice;
        protected int _channelNr;
        private int _note;
        protected Info _midiDeviceInfo;
        protected int _activeSubTrack;
        protected int _currentStep;
        protected int _curMaxStep;
        private int[] _arpeggiator;
        private int _arpIndex;
//        protected List<List<Integer>> _activeSteps;
        protected int[][] _activeSteps;
        protected MidiDevice _midiInDevice;

        private boolean _isMuted;
        private boolean _arpeggiatorOn;
        private Stack<ShortMessage> _noteStack;
        private NoteSelectMidiReceiver _midiReceiver;
        private Transmitter _instrumentSelectTransmitter;

        public TrackModel(int numSteps, int stepsPerBeat, MidiDevice midiInDevice)
        {
            _numberOfSteps = numSteps;
            _stepsPerBeat = stepsPerBeat;
            _noteStack = new Stack<>();
            _midiInDevice = midiInDevice;
            _arpeggiator = new int[]{0, 3, 6};
            _arpIndex = 0;
            _arpeggiatorOn = false;
        }

        public void rewriteNote()
        {
            for (int subTrackIdx = 0; subTrackIdx < _activeSteps.length; subTrackIdx++)
            {
                for (int stepIdx = 0; stepIdx < _activeSteps[subTrackIdx].length; stepIdx++)
                {
                    if(_activeSteps[subTrackIdx][stepIdx] != INACTIVE_SYMBOL)
                    {
                        _activeSteps[subTrackIdx][stepIdx] = _note;
                    }
                }
            }
        }

        public void sendStopped()
        {
            setCurrentStep(0);
            try
            {
                // all notes off
                for(int noteNr = 0; noteNr < 128; noteNr++)
                {
                    ShortMessage offMsg = new ShortMessage();
                    offMsg.setMessage(ShortMessage.NOTE_OFF, _channelNr, noteNr, 0);
                    _midiDevice.getReceiver().send(offMsg, -1);
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
        
        public int getNumberOfSteps()
        {
            return _numberOfSteps;
        }

        public int getStepsPerBeat()
        {
            return _stepsPerBeat;
        }

        public void setDeviceInfo(Info deviceInfo)
        {
            MidiDevice midiDevice;
            try
            {
                midiDevice = MidiSystem.getMidiDevice(deviceInfo);
                if(!midiDevice.isOpen())
                {
                    midiDevice.open();
                }
                _midiDevice = midiDevice;
                _midiDeviceInfo = deviceInfo;
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
        }

        public Info getDeviceInfo()
        {
            return _midiDeviceInfo;
        }

        public int getChannel()
        {
            return _channelNr;
        }

        public void setChannel(int channelNr)
        {
            _channelNr = channelNr;
        }

        public int getNote()
        {
            return _note;
        }

        public void setNote(int note)
        {
            _note = note;
        }

        public void setActiveSubTrack(int activeSubTrack)
        {
            _activeSubTrack = activeSubTrack;
        }

        public void setCurrentStep(int currentStep)
        {
            this._currentStep = currentStep;
        }

        public void setCurrentMaxSteps(int currentMaxSteps)
        {
            _curMaxStep = currentMaxSteps;
        }

        public void createTracks(int maxEventsAtSameTime, int steps)
        {
            _activeSteps = new int[maxEventsAtSameTime][steps];
            for(int curNC = 0; curNC < maxEventsAtSameTime; curNC++)
            {
                for (int stepIdx = 0; stepIdx < _activeSteps[curNC].length; stepIdx++)
                {
                    _activeSteps[curNC][stepIdx] = INACTIVE_SYMBOL; //no note/instrument
                }
            }
        }

        public void initialize()
        {
            setActiveSubTrack(0);
            setCurrentStep(0);
            setCurrentMaxSteps(_numberOfSteps);
            createTracks(MAX_EVENTS_AT_SAME_TIME, _numberOfSteps);
        }

        public boolean isCurrentStep(int stepIdx)
        {
            return stepIdx == this._currentStep;
        }

        public boolean isCurrentMaxStep(int step)
        {
            return step == _curMaxStep;
        }

        public boolean isFirstStepInBeat(int step)
        {
            return step % _stepsPerBeat == 0;
        }

        public int getCurrentMaxSteps()
        {
            return _curMaxStep;
        }

        public void toggleActivationState(int activatedButton)
        {
            if(_activeSteps[_activeSubTrack][activatedButton] == INACTIVE_SYMBOL)
            {
                _activeSteps[_activeSubTrack][activatedButton] = getNote();
            }
            else
            {
                _activeSteps[_activeSubTrack][activatedButton] = INACTIVE_SYMBOL;
            }
        }

        public boolean isStepActive(int stepIdx)
        {
            return _activeSteps[_activeSubTrack][stepIdx] != INACTIVE_SYMBOL;
        }

        public void sendArpeggiator()
        {
            if(_arpeggiatorOn)
            {
                playNote(_activeSteps[_activeSubTrack][_currentStep] + _arpeggiator[_arpIndex]);
                if(_arpIndex == _arpeggiator.length - 1)
                {
                    _arpIndex = 0;
                }
                else
                {
                    _arpIndex++;
                }
            }
        }

        public void sendAdvance(int currentStep)
        {
            playNote(_activeSteps[_activeSubTrack][_currentStep]);
            _currentStep++;
            if(_currentStep >= _curMaxStep)
            {
                _currentStep = 0;
            }
        }

        private void playNote(int noteNumber)
        {
            killOldNotes();
            if(isStepActive(_currentStep) && !isMuted())
            {
                try
                {
                    ShortMessage midiMsg = new ShortMessage();
                    midiMsg.setMessage(ShortMessage.NOTE_ON, _channelNr, noteNumber, 120);
                    _midiDevice.getReceiver().send(midiMsg, -1);
                    _noteStack.push(midiMsg);
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
        }

        protected void killOldNotes()
        {
            try
            {
                while(!_noteStack.empty())
                {
                    ShortMessage oldMsg = _noteStack.pop();
                    int oldChannel = oldMsg.getChannel();
                    int oldNote = oldMsg.getData1();

                    ShortMessage noteOffMsg = new ShortMessage();
                    noteOffMsg.setMessage(ShortMessage.NOTE_OFF, oldChannel, oldNote, 0);
                    _midiDevice.getReceiver().send(noteOffMsg, -1);
                }
            }
            catch (MidiUnavailableException exc1)
            {
                exc1.printStackTrace();
            }
            catch (InvalidMidiDataException exc)
            {
                exc.printStackTrace();
            }
        }

        public boolean isMuted()
        {
            return _isMuted;
        }
        
        public void setMuteStatus(boolean status)
        {
            _isMuted = status;
        }

        public void sendRecording()
        {
        }

        public void sendStopRecording()
        {
        }

        public void sendPaused()
        {
        }

        public void sendPlaying()
        {
        }

        public void openMidiInDevice()
        {
            try
            {
                if(!_midiDevice.isOpen())
                {
                    _midiInDevice.open();
                }
                _instrumentSelectTransmitter = _midiInDevice.getTransmitter();
                if(_midiReceiver == null)
                {
                    _midiReceiver = new NoteSelectMidiReceiver();
                }
                _midiReceiver.setInstrumentSelectingTrack(this);
                _instrumentSelectTransmitter.setReceiver(_midiReceiver);
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
        }

        public void closeNoteSelector()
        {
            _instrumentSelectTransmitter.close();
        }

        public boolean isArpeggiatorOn()
        {
            return _arpeggiatorOn;
        }

        public void setArpeggiator(boolean isOn)
        {
            _arpeggiatorOn = isOn;
        }
    }
    
    public class TracksModel
    {
        private List<TrackModel> _tracksModels;
        private MidiDevice _midiInDevice;

        public TracksModel(int numTracks, int steps, int stepsPerBeat, int beatsPerMinute, MidiDevice midiDevice)
        {
            _midiInDevice = midiDevice;
            _tracksModels = new ArrayList<TrackModel>();
            for(int trackCnt = 0; trackCnt < numTracks; trackCnt++)
            {
                
                TrackModel newModel = null;
                newModel = new NoteLooperModel(steps, stepsPerBeat, beatsPerMinute, _midiInDevice);
                _tracksModels.add(newModel);
            }
            mapMidi(_tracksModels);
            for (TrackModel curTrackModel : _tracksModels)
            {
                curTrackModel.initialize();
            }
        }

        public void arpeggiator()
        {
            for (TrackModel curMod : _tracksModels)
            {
                curMod.sendArpeggiator();
            }
        }

        public List<TrackModel> getTrackModels()
        {
            return _tracksModels;
        }
        
        public void sendAdvance(int currentStep)
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _tracksModels.get(trackCnt).sendAdvance(currentStep);
            }
        }
        
        public void sendStopped()
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _tracksModels.get(trackCnt).sendStopped();
            }
        }

        public void sendRecording()
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _tracksModels.get(trackCnt).sendRecording();
            }
        }

        public void sendPaused()
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _tracksModels.get(trackCnt).sendPaused();
            }
        }

        public void sendPlaying()
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _tracksModels.get(trackCnt).sendPlaying();
            }
        }

        public MidiDevice getMidiInDevice()
        {
            return _midiInDevice;
        }
    }

    public class TracksScreen implements Screen
    {
        private List<ScreenElement> _elements;
        private SequencerMain _parent;
        private SequencerBarArea _sequencerBarsArea;
        private TracksModel _tracksModel;
        
        public TracksScreen(SequencerMain parent, TracksModel tracksModel)
        {
            _elements = new ArrayList<>();
            _parent = parent;
            this._tracksModel = tracksModel;
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
            SequencerBarArea sequencerBarsArea = new SequencerBarArea(_parent, new Rectangle(100, 10, width - 120, height - 100), this._tracksModel);
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
    
    public class NoteSelectMidiReceiver implements Receiver
    {
        private TrackModel _instrumentSelectingTrack;

        @Override
        public void send(MidiMessage message, long timeStamp)
        {
            if(message instanceof ShortMessage)
            {
                ShortMessage sMessage = (ShortMessage)message;
                System.out.println("timestamp: " + timeStamp + " Message: " + midiMessageToString(sMessage));
                _instrumentSelectingTrack.setNote(sMessage.getData1());
            }
        }

        @Override
        public void close()
        {
        }

        public void setInstrumentSelectingTrack(TrackModel intstrumentSelectingTrack)
        {
            _instrumentSelectingTrack = intstrumentSelectingTrack;
        }
    }

    public class InstrumentSelectScreen implements Screen
    {
        private List<Info> _devices;
        private List<ScreenElement> _elements;
        private SequencerMain _mainApp;
        private TrackModel _instrumentSelectingTrack;
        private InputState _inputState;

        public InstrumentSelectScreen(SequencerMain sequencerMain, InputState inputState)
        {
            _mainApp = sequencerMain;
            MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
            _devices = new ArrayList<>();
            for (Info curDevice : deviceInfos)
            {
                _devices.add(curDevice);
            }
            _elements = new ArrayList<>();
        }
        
        public void setSelectingTrack(TrackModel intstrumentSelectingTrack)
        {
            _instrumentSelectingTrack = intstrumentSelectingTrack;
            _instrumentSelectingTrack.openMidiInDevice();
        }

        @Override
        public void create()
        {
            int xDevicePos = 20;
            int yDevicePos = 20;
            for (Info curDev : _devices)
            {
                _elements.add(new DeviceButton(_mainApp, xDevicePos, yDevicePos, curDev, _playStatus, _inputState, _instrumentSelectingTrack));
                yDevicePos = yDevicePos  + 30;
            }
            int xChannelPos = 500;
            int yChannelPos = 20;
            for(int channelNr = 0; channelNr < 16; channelNr++)
            {
                _elements.add(new ChannelSelectButton(_mainApp, xChannelPos, yChannelPos, channelNr, _playStatus, _inputState, _instrumentSelectingTrack));
                yChannelPos = yChannelPos + 30;
            }
                
            int xNotePos = 600;
            int yNotePos = 20;
            _elements.add(new MidiReceiveLabel(_mainApp, xNotePos, yNotePos, _instrumentSelectingTrack));
            
            int xStartPos = 600;
            int yStartPos = 60;
            createPresetNoteSelectButtons(_mainApp, xStartPos, yStartPos, _instrumentSelectingTrack);
        }

        private void createPresetNoteSelectButtons(SequencerMain mainApp, int xStartPos, int yStartPos, TrackModel instrumentSelectingTrack)
        {
            Map<Integer, String> noteInstrumentMapping = new HashMap<>();
            noteInstrumentMapping.put(36, "Kick");
            noteInstrumentMapping.put(38, "Snare");
            noteInstrumentMapping.put(50, "Lo Tom");
            noteInstrumentMapping.put(42, "Hi Tom");
            noteInstrumentMapping.put(42, "Cl Hat");
            noteInstrumentMapping.put(46, "Op Hat");
            noteInstrumentMapping.put(39, "Clap");
            noteInstrumentMapping.put(75, "Claves");
            noteInstrumentMapping.put(67, "Agogo");
            noteInstrumentMapping.put(49, "Crash");
            
            int xPos = xStartPos;
            int yPos = yStartPos;
            Set<Integer> noteSet = noteInstrumentMapping.keySet();
            for (Integer curNote : noteSet)
            {
                _elements.add(new NoteSelectButton(mainApp, xPos, yPos, curNote, noteInstrumentMapping.get(curNote), _instrumentSelectingTrack));
                yPos += 30;
            }
        }

        @Override
        public void add(ScreenElement element)
        {
            _elements.add(element);
        }

        @Override
        public void mousePressed(MouseEvent event, InputState inputState)
        {
            for (ScreenElement curElem : _elements)
            {
                curElem.mousePressed(event, inputState);
            }
        }

        @Override
        public void draw(DrawType type)
        {
            for (ScreenElement curElem : _elements)
            {
                if(curElem instanceof InstrumentSelectButton)
                {
                    InstrumentSelectButton curButton = (InstrumentSelectButton)curElem;
                    curButton.setActiveTrack(_instrumentSelectingTrack);
                }
                curElem.draw(type);
            }
        }
    }

    public class DeviceButton extends InstrumentSelectButton
    {
        private Info _deviceInfo;

        public DeviceButton(SequencerMain mainApp, int x, int y, Info deviceInfo, PlayStatus playStatus, InputState inputState, TrackModel instrumentSelectingTrack)
        {
            super(mainApp, new Rectangle(x, y, 400, 25), playStatus, inputState, instrumentSelectingTrack);
            _deviceInfo = deviceInfo;
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _trackModel.setDeviceInfo(_deviceInfo);
            inputState.instrumentSelected();
        }

        @Override
        protected void setColor()
        {
            if(_trackModel.getDeviceInfo() == _deviceInfo)
            {
                _mainApp.fill(64, 255, 255);
            }
            else
            {
                _mainApp.fill(0, 128, 128);
            }
        }

        @Override
        public void draw(DrawType type)
        {
            super.draw(type);
            textFont(_instrumentSelectFont);
            textAlign(LEFT);
            int previousColor = getGraphics().fillColor;
            fill(0);
            text("N: " + _deviceInfo.getName() + " D: " + _deviceInfo.getDescription(), _area.x + 10, _area.y + 18);
            fill(previousColor);
        }
    }
    
    public class ChannelSelectButton extends InstrumentSelectButton
    {
        private int _channelNr;

        public ChannelSelectButton(SequencerMain mainApp, int x, int y, int channelNr, PlayStatus playStatus, InputState inputState, TrackModel trackModel)
        {
            super(mainApp, new Rectangle(x, y, 50, 25), playStatus, inputState, trackModel);
            _channelNr = channelNr;
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _trackModel.setChannel(_channelNr);
            inputState.instrumentSelected();
        }

        @Override
        public void draw(DrawType type)
        {
            super.draw(type);
            textFont(_instrumentSelectFont);
            textAlign(LEFT);
            int previousColor = getGraphics().fillColor;
            fill(0);
            text(_channelNr, _area.x + 10, _area.y + 18);
            fill(previousColor);
        }
        @Override
        protected void setColor()
        {
            if(_trackModel.getChannel() == _channelNr)
            {
                _mainApp.fill(128, 128, 255);
            }
            else
            {
                _mainApp.fill(64, 64, 255);
            }
        }
    }
    
    public class MidiReceiveLabel extends InstrumentSelectButton
    {
        public MidiReceiveLabel(SequencerMain mainApp, int xPos, int yPos, TrackModel trackModel)
        {
            super(mainApp, new Rectangle(xPos, yPos, 50, 25), null, null, trackModel);
        }

        @Override
        public void draw(DrawType type)
        {
            super.draw(type);
            textFont(_instrumentSelectFont);
            textAlign(LEFT);
            int previousColor = getGraphics().fillColor;
            fill(0);
            text(_trackModel.getNote(), _area.x + 10, _area.y + 18);
            fill(previousColor);
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            inputState.instrumentSelected();
        }

        @Override
        protected void setColor()
        {
            fill(255, 128, 255);
        }
    }

    public class NoteSelectButton extends InstrumentSelectButton
    {
        private int _note;
        private String _instrumentName;

        public NoteSelectButton(SequencerMain mainApp, int xPos, int yPos, int note, String instrumentName, TrackModel instrumentSelectingTrack)
        {
            super(mainApp, new Rectangle(xPos, yPos, 100, 25), null, null, instrumentSelectingTrack);
            _note = note;
            _instrumentName = instrumentName;
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _trackModel.setNote(_note);
            inputState.instrumentSelected();
        }

        @Override
        public void draw(DrawType type)
        {
            super.draw(type);
            textFont(_instrumentSelectFont);
            textAlign(LEFT);
            int previousColor = getGraphics().fillColor;
            fill(0);
            text(_note + ": " + _instrumentName, _area.x + 10, _area.y + 18);
            fill(previousColor);
        }

        @Override
        protected void setColor()
        {
            fill(0, 128, 255);
        }
    }
    
    public class LooperReceiver implements Receiver
    {
        private NoteLooperModel _looperModel;

        public LooperReceiver(NoteLooperModel noteLooperModel)
        {
            _looperModel = noteLooperModel;
        }

        @Override
        public void send(MidiMessage message, long timeStamp)
        {
            if(_looperModel.isRecording() && (message instanceof ShortMessage))
            {
                ShortMessage sMessage = (ShortMessage)message;
                System.out.print("Got message: " + midiMessageToString(sMessage));
                if(sMessage.getData2() != 0)
                {
                    System.out.println("recording it!");
                    _looperModel.recordNote(message);
                }
                else
                {
                    System.out.println("");
                }
            }
        }

        @Override
        public void close()
        {
        }
    }

    public class ArpeggiatorButton extends SeqButton
    {
        private TrackModel _trackModel;

        public ArpeggiatorButton(SequencerMain mainApp, Rectangle rectangle, TrackModel trackModel)
        {
            super(mainApp, rectangle, null, null);
            _trackModel = trackModel;
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _trackModel.setArpeggiator(!_trackModel.isArpeggiatorOn());
            System.out.println("arp button pressed");
        }

        @Override
        protected void setColor()
        {
            if(_trackModel.isArpeggiatorOn())
            {
                _mainApp.fill(255, 0, 0);
            }
            else
            {
                _mainApp.fill(0, 255, 0);
            }
        }
    }

    public class RecoredButton extends SeqButton
    {
        private NoteLooperModel _trackModel;

        public RecoredButton(SequencerMain mainApp, Rectangle rectangle, TrackModel trackModel)
        {
            super(mainApp, rectangle, null, null);
            _trackModel = (NoteLooperModel)trackModel;
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            if(!_trackModel.isRecording())
            {
                _trackModel.sendRecording();
            }
            else
            {
                _trackModel.sendStopRecording();
            }
        }

        @Override
        public void draw(DrawType type)
        {
            super.draw(type);
            int prevCol = _mainApp.getGraphics().fillColor;
            boolean prevStroke = _mainApp.getGraphics().stroke;
            int prevStrokeCol = _mainApp.getGraphics().strokeColor;
            _mainApp.noStroke();
            _mainApp.fill(0, 0, 0);
            _mainApp.ellipse(_area.x + ((int)_area.width/2) + 1, _area.y + ((int)_area.height/2)  + 1, _area.width/3 - 1, _area.height/3);
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

        @Override
        protected void setColor()
        {
            if(_trackModel.isRecording())
            {
                _mainApp.fill(255, 10, 10);
            }
            else
            {
                _mainApp.fill(200, 10, 10);
            }
        }
    }

    public class NoteLooperBar extends StepSequencerBar
    {
        private RecoredButton _recordButton;
        private ArpeggiatorButton _arpeggiatorButton;

        public NoteLooperBar(Rectangle barArea, PVector insets, TrackModel trackModel, SequencerMain mainApp)
        {
            super(barArea, insets, trackModel, mainApp);
            int insideButtonHeight = (int)(_buttonHeight / 2.4);
            _recordButton = new RecoredButton(mainApp, new Rectangle(barArea.x - 35, barArea.y + 5, 35, insideButtonHeight), trackModel);
            _arpeggiatorButton = new ArpeggiatorButton(mainApp, new Rectangle(barArea.x - 35, barArea.y + 15 + insideButtonHeight, 35, insideButtonHeight), trackModel);
        }

        @Override
        public void draw(DrawType type)
        {
            super.draw(type);
            _recordButton.draw(type);
            _arpeggiatorButton.draw(type);
        }

        @Override
        public void mousePressed(MouseEvent event, InputState inputState)
        {
            super.mousePressed(event, inputState);
            _recordButton.mousePressed(event, inputState);
            _arpeggiatorButton.mousePressed(event, inputState);
        }
    }

    public class NoteLooperModel extends TrackModel
    {
        private PlayStatusType _loopingState;

        public NoteLooperModel(int steps, int stepsPerBeat, int beatsPerMinute, MidiDevice midiInDevice)
        {
            super(steps, stepsPerBeat, midiInDevice);
            _loopingState = PlayStatusType.STOPPED;
            try
            {
                if(!midiInDevice.isOpen())
                {
                    midiInDevice.open();
                }
                Transmitter recordingTransmitter = midiInDevice.getTransmitter();
                Receiver loopReceiver = new LooperReceiver(this);
                recordingTransmitter.setReceiver(loopReceiver);
            }
            catch (MidiUnavailableException exc)
            {
                exc.printStackTrace();
            }
        }

        @Override
        public void rewriteNote()
        {
            //we don't react to a note change because we record notes by keyboard
        }

        public void recordNote(MidiMessage message)
        {
            _activeSteps[_activeSubTrack][_currentStep] = ((ShortMessage)message).getData1();
        }

        public boolean isRecording()
        {
            return _loopingState == PlayStatusType.RECORDING;
        }

        @Override
        public void sendStopped()
        {
            switch (_loopingState)
            {
                case RECORDING:
                case PLAYING:
                    break;
                default:
                    break;
            }
            _loopingState = PlayStatusType.STOPPED;
            super.sendStopped();
        }

        @Override
        public void sendRecording()
        {
            _loopingState = PlayStatusType.RECORDING;
        }

        @Override
        public void sendStopRecording()
        {
            _loopingState = PlayStatusType.PLAYING;
            super.sendStopRecording();
        }

        @Override
        public void sendPaused()
        {
            _loopingState = PlayStatusType.PAUSED;
            super.sendPaused();
        }

        @Override 
        public void sendPlaying()
        {
            if(_loopingState != PlayStatusType.RECORDING)
            {
                _loopingState = PlayStatusType.PLAYING;
            }
            super.sendPlaying();
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
