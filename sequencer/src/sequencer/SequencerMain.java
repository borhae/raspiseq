package sequencer;

import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

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
import sequencer.SequencerMain.BeatGeneratingTimerTask;

public class SequencerMain extends PApplet
{
    private static final String INSTRUMENT_SELECT_SCREEN_ID = "instrumentSelect";
    private static final String TRACK_SCREEN_ID = "trackScreen";
    private static final int STEPS_PER_BEAT = 4;
    private static final int STEPS = 32;
    private static final int NUM_TRACKS = 8;
    
    private long _stepTimer;
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
    private Queue<MidiNoteInfo> _noteStack;
    private static final List<Integer> ARPEGGIATOR_NOTE_SEQUENCE = Arrays.asList(new Integer[]{3, 5, 8});
    
    public static void main(String[] args)
    {
        PApplet.main("sequencer.SequencerMain");
    }
    
    @Override
    public void settings()
    {
        size(1920, 1080);
//        fullScreen();
    }

    @Override
    public void setup()
    {
        System.out.println("setup time");
        _stepTimer = 0;
        _passedTime = 0;
        _beatsPerMinute = 125;
        _oldTime = 0;
        _currentStep = 0;
        _millisToPass = 60000 / (_beatsPerMinute * STEPS_PER_BEAT);
        System.out.println("millis per step: " + _millisToPass);

        _counterFont = createFont("Arial", 48, true);
        _instrumentSelectFont = createFont("Arial", 12, true);

        _playStatus = new PlayStatus(PlayStatusType.STOPPED);

        _inputState = new InputState();
        _inputState.setState(InputStateType.REGULAR);

        
        MidiDevice midiInDevice = null;
        Info[] midiDeviceInfos = null;
        List<MidiDevice> inDevices = new ArrayList<>();
        List<MidiDevice> outDevices = new ArrayList<>();
        midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
        try
        {
            for (Info curDevice : midiDeviceInfos)
            {
                MidiDevice midiDevice = MidiSystem.getMidiDevice(curDevice);
                int maxReceivers = midiDevice.getMaxReceivers();
                int maxTransmitters = midiDevice.getMaxTransmitters();
                if(maxReceivers == 0 && maxTransmitters != 0)
                {
                    inDevices.add(midiDevice);
                    System.out.println("Device name: " + curDevice.getName() + ", description: " 
                            + curDevice.getDescription() + ", version: " + curDevice.getVersion() + ", vendor: " + curDevice.getVendor());
                    System.out.println("-------Provides: input device");
                 }
                else if (maxReceivers != 0  && maxTransmitters == 0)
                {
                    outDevices.add(midiDevice);
                    System.out.println("Device name: " + curDevice.getName() + ", description: " 
                            + curDevice.getDescription() + ", version: " + curDevice.getVersion() + ", vendor: " + curDevice.getVendor());
                    System.out.println("-------Provides: output device");
                }
                else
                {
                    System.out.println("Device name: " + curDevice.getName() + ", description: " 
                            + curDevice.getDescription() + ", version: " + curDevice.getVersion() + ", vendor: " + curDevice.getVendor());
                    System.out.println("-------Provides: " + maxReceivers + " receivers and " + maxTransmitters + " transmitters");
                    System.out.println("");
                }
            }
            midiInDevice = createMidiInDevice(inDevices);
            _noteStack = new ArrayDeque<>(64);
            _tracksModel = new TracksModel(NUM_TRACKS, STEPS, STEPS_PER_BEAT, midiInDevice, _noteStack, outDevices); 
        }
        catch (MidiUnavailableException exc)
        {
            exc.printStackTrace();
        }
        
        _screens = new HashMap<>();
        TracksScreen tracksScreen = new TracksScreen(this, _tracksModel);
        tracksScreen.create();

        InstrumentSelectScreen  instrumentSelectScreen = new InstrumentSelectScreen(this, _inputState, outDevices);
        instrumentSelectScreen.create();
        
        _screens.put(TRACK_SCREEN_ID, tracksScreen);
        _screens.put(INSTRUMENT_SELECT_SCREEN_ID, instrumentSelectScreen);
        
        _currentScreen = tracksScreen;
        _currentScreen.draw(DrawType.NO_STEP_ADVANCE);
        TimerTask beatGenerator = new BeatGeneratingTimerTask();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(beatGenerator, 0, _millisToPass);
//        timer.scheduleAtFixedRate(beatGenerator, 0, 1000);
        noLoop();
    }
    
    public class BeatGeneratingTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            redraw();
            generateBeat();
        }
    }

    public static String midiMessageToString(ShortMessage sMessage)
    {
        return "Channel: " + sMessage.getChannel() + ", Command: " + sMessage.getCommand() + ", Data1: " + sMessage.getData1() + ", Data2: " + sMessage.getData2() + ", Length: " + sMessage.getLength() + ", Status: " + sMessage.getStatus();
    }

    private MidiDevice createMidiInDevice(List<MidiDevice> inDevices) 
    {
        MidiDevice midiInDevice = null;
        for (MidiDevice curDevice : inDevices)
        {
//            System.out.print("Name: " + curDevice.getName() + "  Description: " + curDevice.getDescription());
//            if(curDevice.getName().equals("K32 [hw:1,0,0]") && curDevice.getDescription().equals("Keystation Mini 32, USB MIDI, Keystation Mini 32") )
//            {
//                System.out.println("found input device");
//                midiInDevice = curDevice;
//                return MidiSystem.getMidiDevice(midiInDevice);
//            }
            Info deviceInfo = curDevice.getDeviceInfo();
            if(deviceInfo.getName().equals("Keystation Mini 32") && deviceInfo.getDescription().equals("No details available") )
            {
                midiInDevice = curDevice;
            }
        }
        return midiInDevice;
    }

    private void mapMidi(List<TrackModel> tracksModels, List<MidiDevice> outDevices)
    {
            homeMapping(tracksModels, outDevices);
//            windowsMapping(tracksModels);
            System.out.println();
    }
 
    private void windowsMapping(List<TrackModel> trackModels, List<MidiDevice> outDevices)
    {
        int channel = 10;
        MidiDevice primaryMidiOutDevice = null;
        for (MidiDevice curDevice : outDevices)
        {
            Info info = curDevice.getDeviceInfo();
            System.out.print("Name: " + info.getName());
            System.out.print("  Description: " + info.getDescription());
            if(info.equals("Gervill") && info.getDescription().equals("Software MIDI Synthesizer"))
            {
                primaryMidiOutDevice = curDevice;
                System.out.print(" <---- SELECTED");
            }                                                                                                                           
        }
        for (TrackModel trackModel : trackModels)
        {
            trackModel.setDevice(primaryMidiOutDevice);
            trackModel.setChannel(channel);
        }
        System.out.println();
        trackModels.get(0).setNote(36);
        trackModels.get(1).setNote(38);
        trackModels.get(2).setNote(39);
        trackModels.get(3).setNote(42);
        trackModels.get(4).setNote(43);
        trackModels.get(5).setNote(46);
        trackModels.get(6).setNote(50);
        trackModels.get(7).setNote(75);
    }

    private void homeMapping(List<TrackModel> trackModels, List<MidiDevice> outDevices)
    {
        System.out.println("doing home mapping:");
        // String outputDeviceName = "U4i4o [hw:2,0,0]"; // Linux driver name
        String outputDeviceName = "USB Midi 4i4o"; // Windows driver name
        // Linux: description is null
        String outputDeviceDescription = "External MIDI Port"; // Windows driver
                                                               // description
        int channel = 0;
        MidiDevice primaryMidiOutDevice = null;
        for (MidiDevice curDevice : outDevices)
        {
            Info info = curDevice.getDeviceInfo();
            System.out.print("is: " + info.getName());
            System.out.print(", looking for: " + outputDeviceName);
            if (info.getName().equals(outputDeviceName) && info.getDescription().equals(outputDeviceDescription))
            {
                primaryMidiOutDevice = curDevice;
                System.out.print(" <---- SELECTED");
            }
            System.out.println("");
        }
        for (TrackModel trackModel : trackModels)
        {
            trackModel.setDevice(primaryMidiOutDevice);
            trackModel.setChannel(channel);
        }
        trackModels.get(0).setNote(36);
        trackModels.get(1).setNote(38);
        trackModels.get(2).setNote(39);
        trackModels.get(3).setNote(42);
        trackModels.get(4).setNote(43);
        trackModels.get(5).setNote(46);
        trackModels.get(6).setNote(50);
        trackModels.get(7).setNote(75);
    }

    @Override
    public void draw()
    {
        _currentScreen.draw(DrawType.STEP_ADVANCE);
    }
    
    public void generateBeat()
    {
        killOldNotes();
        switch (_playStatus.getStatus())
        {
            case STOPPED:
                _currentStep = 0;
                _tracksModel.sendStopped();
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
    }

    private void oldDraw()
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
            killOldNotes();
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
//            showCurrentStepAsNumber();
        }
        else
        {
            _stepTimer = _stepTimer - _passedTime;
        }
    }
    
    protected void killOldNotes()
    {
        try
        {
            while(!_noteStack.isEmpty())
            {
                MidiNoteInfo noteInfoToRemove = _noteStack.remove();
                ShortMessage oldMsg = noteInfoToRemove.getMidiMsg();
                int oldChannel = oldMsg.getChannel();
                int oldNote = oldMsg.getData1();

                ShortMessage noteOffMsg = new ShortMessage();
                noteOffMsg.setMessage(ShortMessage.NOTE_OFF, oldChannel, oldNote, 0);
                noteInfoToRemove.getOutDevice().getReceiver().send(noteOffMsg, -1);
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
        redraw();
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
            System.out.println("status set to: " + status);
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
        protected int _numberOfSteps;
        protected int _stepsPerBeat;
        protected MidiDevice _midiOutDevice;
        protected int _channelNr;
        private int _note;
        protected Info _midiDeviceInfo;
        protected int _activeSubTrack;
        protected int _currentStep;
        protected int _curMaxStep;
        protected List<List<Integer>> _activeSteps;
        protected MidiDevice _midiInDevice;

        private boolean _isMuted;
        private boolean _arpeggiatorOn;
        private Queue<MidiNoteInfo> _noteStack;
        private NoteSelectMidiReceiver _midiReceiver;
        private Transmitter _instrumentSelectTransmitter;

        private Stack<Integer> _arpeggiator;


        public TrackModel(int numSteps, int stepsPerBeat, MidiDevice midiInDevice, Queue<MidiNoteInfo> noteStack)
        {
            _numberOfSteps = numSteps;
            _stepsPerBeat = stepsPerBeat;
            _noteStack = noteStack;
            _midiInDevice = midiInDevice;
            _arpeggiatorOn = false;
            _arpeggiator = new Stack<>();
        }

        public void rewriteNote()
        {
            int numOfSteps = _activeSteps.size();
            for (int stepIdx = 0; stepIdx < numOfSteps; stepIdx++)
            {
                if(isStepActive(stepIdx))
                {
                    _activeSteps.get(stepIdx).add(_note);
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
                    _midiOutDevice.getReceiver().send(offMsg, -1);
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

        public void setDevice(MidiDevice primaryMidiOutDevice)
        {
            try
            {
                if(!primaryMidiOutDevice.isOpen())
                {
                    primaryMidiOutDevice.open();
                }
                _midiOutDevice = primaryMidiOutDevice;
                _midiDeviceInfo = primaryMidiOutDevice.getDeviceInfo();
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

        public void createTracks(int steps)
        {
            _activeSteps = new ArrayList<>();
            for(int stepIdx = 0; stepIdx < steps; stepIdx++)
            {
                _activeSteps.add(new ArrayList<>()); // empty list is no note
            }
        }

        public void initialize()
        {
            setActiveSubTrack(0);
            setCurrentStep(0);
            setCurrentMaxSteps(_numberOfSteps);
            createTracks(_numberOfSteps);
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
            if(_activeSteps.get(activatedButton).isEmpty())
            {
                _activeSteps.get(activatedButton).add(getNote());
            }
            else
            {
                _activeSteps.get(activatedButton).clear();
            }
        }

        public boolean isStepActive(int stepIdx)
        {
            return !_activeSteps.get(stepIdx).isEmpty();
        }

        public void sendAdvance(int currentStep)
        {
            if (!isMuted())
            {
                if (isStepActive(_currentStep))
                {
                    Integer currentNote = _activeSteps.get(_currentStep).get(0);
                    playNote(currentNote);
                    reloadArpeggiator(currentNote);
                }
                else if (_arpeggiatorOn && !_arpeggiator.empty())
                {
                    playNote(_arpeggiator.pop().intValue());
                }
            }
            _currentStep++;
            if (_currentStep >= _curMaxStep)
            {
                _currentStep = 0;
            }
        }

        private void reloadArpeggiator(Integer currentNote)
        {
            _arpeggiator.clear();
            for (Integer curArpNote : ARPEGGIATOR_NOTE_SEQUENCE)
            {
                _arpeggiator.push(currentNote + curArpNote);
            }
        }

        private void playNote(int noteNumber)
        {
            try
            {
                ShortMessage midiMsg = new ShortMessage();
                midiMsg.setMessage(ShortMessage.NOTE_ON, _channelNr, noteNumber, 120);
                _midiOutDevice.getReceiver().send(midiMsg, -1);
                _noteStack.add(new MidiNoteInfo(_midiOutDevice, midiMsg));
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
                if(!_midiInDevice.isOpen())
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

        public TracksModel(int numTracks, int steps, int stepsPerBeat, MidiDevice midiDevice, Queue<MidiNoteInfo> noteStack, List<MidiDevice> outDevices)
        {
            _midiInDevice = midiDevice;
            _tracksModels = new ArrayList<TrackModel>();
            for(int trackCnt = 0; trackCnt < numTracks; trackCnt++)
            {
                
                TrackModel newModel = null;
                newModel = new NoteLooperModel(steps, stepsPerBeat, _midiInDevice, noteStack);
                _tracksModels.add(newModel);
            }
            mapMidi(_tracksModels, outDevices);
            for (TrackModel curTrackModel : _tracksModels)
            {
                curTrackModel.initialize();
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
        private List<MidiDevice> _devices;
        private List<ScreenElement> _elements;
        private SequencerMain _mainApp;
        private TrackModel _instrumentSelectingTrack;
        private InputState _inputState;

        public InstrumentSelectScreen(SequencerMain sequencerMain, InputState inputState, List<MidiDevice> outDevices)
        {
            _mainApp = sequencerMain;
            _devices = outDevices;
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
            for (MidiDevice curDev : _devices)
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
        private MidiDevice _device;
        private Info _deviceInfo;

        public DeviceButton(SequencerMain mainApp, int x, int y, MidiDevice device, PlayStatus playStatus, InputState inputState, TrackModel instrumentSelectingTrack)
        {
            super(mainApp, new Rectangle(x, y, 400, 25), playStatus, inputState, instrumentSelectingTrack);
            _device = device;
            _deviceInfo = device.getDeviceInfo();
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
            _trackModel.setDevice(_device);
            inputState.instrumentSelected();
        }

        @Override
        protected void setColor()
        {
            if(_trackModel.getDeviceInfo() == _device)
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

        public NoteLooperModel(int steps, int stepsPerBeat, MidiDevice midiInDevice, Queue<MidiNoteInfo> noteStack)
        {
            super(steps, stepsPerBeat, midiInDevice, noteStack);
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
                System.out.println("For this Midi Device:" + midiInDevice.getDeviceInfo().getName());
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
            int note = ((ShortMessage)message).getData1();
            _activeSteps.get(_currentStep).add(note);
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

    public class MidiNoteInfo
    {
        private MidiDevice _midOutDevice;
        private ShortMessage _midiMsg;

        public MidiNoteInfo(MidiDevice midiOutDevice, ShortMessage midiMsg)
        {
            _midOutDevice = midiOutDevice;
            _midiMsg = midiMsg;
        }

        public MidiDevice getOutDevice()
        {
            return _midOutDevice;
        }

        public ShortMessage getMidiMsg()
        {
            return _midiMsg;
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
