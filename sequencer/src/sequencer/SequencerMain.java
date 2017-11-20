package sequencer;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;
import processing.event.MouseEvent;
import sequencer.SequencerBar.MidiInstrumentType;

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
    private SequencerBarArea _sequencerBarsArea;
    private InputState _inputState;
    private Screen _currentScreen;
    private PlayStatus _playStatus;
    private Map<String, Screen> _screens;
    private List<TrackModel> _tracksModels;
    
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

        _counterFont = createFont("Arial", 48, true);
        _instrumentSelectFont = createFont("Arial", 12, true);

        _playStatus = new PlayStatus(PlayStatusType.STOPPED);

        _inputState = new InputState();
        _inputState.setState(InputStateType.REGULAR);

        _tracksModels = new ArrayList<TrackModel>();
        for(int trackCnt = 0; trackCnt < NUM_TRACKS; trackCnt++)
        {
            _tracksModels.add(new TrackModel(STEPS, STEPS_PER_BEAT));
        }
        mapMidi(_tracksModels);
        
        _screens = new HashMap<>();
        TracksScreen tracksScreen = new TracksScreen(this, _tracksModels);
        tracksScreen.create();
        _sequencerBarsArea = tracksScreen.getSequencerArea();
        
        InstrumentSelectScreen  instrumentSelectScreen = new InstrumentSelectScreen(this);
        instrumentSelectScreen.create();
        
        _screens.put(TRACK_SCREEN_ID, tracksScreen);
        _screens.put(INSTRUMENT_SELECT_SCREEN_ID, instrumentSelectScreen);
        
        _currentScreen = tracksScreen;
        _currentScreen.draw(DrawType.NO_STEP_ADVANCE);
        
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
                    trackModel.setInstrumentType(SequencerBar.MidiInstrumentType.SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS);
                }
                System.out.print(" <---- SELECTED");
            }
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
        int channel = 10;
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
                    trackModel.setInstrumentType(SequencerBar.MidiInstrumentType.SINGLE_CHANNEL_MULTIPLE_INSTRUMENTS);
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
                    _playStatus.hasStopped();
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
            background(255);
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
            }
        }

        public void instrumentSelectedPressed()
        {
            if(_state == InputStateType.INSTRUMENT_SELECT_ACTIVE)
            {
                _state = InputStateType.REGULAR;
                background(255);
                _currentScreen = _screens.get(TRACK_SCREEN_ID);
            }
        }

        public void channelSelectPressed()
        {
            if(_state == InputStateType.INSTRUMENT_SELECT_ACTIVE)
            {
                _state = InputStateType.REGULAR;
                background(255);
                _currentScreen = _screens.get(TRACK_SCREEN_ID);
            }
        }

        public void noteSelected()
        {
            // TODO Auto-generated method stub
            
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
            
        public void hasStopped()
        {
            _status = PlayStatusType.PAUSED;
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
        private List<SequencerBar> _sequencerBars;

        public SequencerBarArea(SequencerMain mainApp, Rectangle area, List<TrackModel> tracksModels)
        {
            PVector insets = new PVector(10, 5);
            _sequencerBars = new ArrayList<>();
            int trackHeight = (int)SequencerBar.computHeight(area.height, tracksModels.size(), insets.y);
            int cnt = 0;
            for (TrackModel trackModel : tracksModels)
            {
                SequencerBar newTrac = 
                        new SequencerBar(new PVector(area.x, area.y + cnt * trackHeight), insets, area.width, trackHeight, trackModel, cnt, mainApp);
                _sequencerBars.add(newTrac);
                cnt++;
            }
            draw(DrawType.NO_STEP_ADVANCE);
        }

        public void sendAdvance(int currentStep)
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _sequencerBars.get(trackCnt).sendAdvance(currentStep);
            }
        }
        
        public void sendStopped()
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _sequencerBars.get(trackCnt).sendStopped();
            }
        }

        @Override
        public void draw(DrawType type)
        {
            if(type == DrawType.STEP_ADVANCE)
            {
                for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
                {
                    _sequencerBars.get(trackCnt).draw(type);
                }
            }
        }

        public void mousePressed(MouseEvent event, InputState inputState)
        {
            for(int trackCnt = 0; trackCnt < _tracksModels.size(); trackCnt++)
            {
                _sequencerBars.get(trackCnt).mousePressed(event, inputState);
            }
        }
    }
    
    public class TrackModel
    {
        private int _numberOfSteps;
        private int _stepsPerBeat;
        private MidiDevice _midiDevice;
        private int _channelNr;
        private int _note;
        private Info _midiDeviceInfo;
        private SequencerBar.MidiInstrumentType _instrumentType;

        public TrackModel(int numSteps, int stepsPerBeat)
        {
            _numberOfSteps = numSteps;
            _stepsPerBeat = stepsPerBeat;
        }

        public void setInstrumentType(MidiInstrumentType instrumentType)
        {
            _instrumentType = instrumentType;
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

        public MidiDevice getMidiDevice()
        {
            return _midiDevice;
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

        public SequencerBar.MidiInstrumentType getInstrumentType()
        {
            return _instrumentType;
        }
    }

    public class TracksScreen implements Screen
    {
        private List<ScreenElement> _elements;
        private SequencerMain _parent;
        private SequencerBarArea _sequencerBarsArea;
        private List<TrackModel> _tracksModels;
        
        public TracksScreen(SequencerMain parent, List<TrackModel> tracksModels)
        {
            _elements = new ArrayList<>();
            _parent = parent;
            this._tracksModels = tracksModels;
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
            SequencerBarArea sequencerBarsArea = new SequencerBarArea(_parent, new Rectangle(100, 10, width - 120, height - 100), this._tracksModels);
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
        private List<ScreenElement> _elements;
        private SequencerMain _mainApp;
        private TrackModel _instrumentSelectingTrack;
        private MidiDevice _midiInDevice;

        public InstrumentSelectScreen(SequencerMain sequencerMain)
        {
            _mainApp = sequencerMain;
            MidiDevice.Info[] deviceInfos = MidiSystem.getMidiDeviceInfo();
            _devices = new ArrayList<>();
            for (Info curDevice : deviceInfos)
            {
                _devices.add(curDevice);
            }
            _elements = new ArrayList<>();
            _midiInDevice = null;
            for (Info curDevice : deviceInfos)
            {
                if(curDevice.getName().equals("Keystation Mini 32") && curDevice.getDescription().equals("No details available") )
                {
                    try
                    {
                        _midiInDevice = MidiSystem.getMidiDevice(curDevice);
                        _midiInDevice.open();
                        Transmitter myTransmitter = _midiInDevice.getTransmitter();
                        myTransmitter.setReceiver(new Receiver() {
                            
                            @Override
                            public void send(MidiMessage message, long timeStamp)
                            {
                                if(message instanceof ShortMessage)
                                {
                                    ShortMessage sMessage = (ShortMessage)message;
                                    System.out.println("timestamp: " + timeStamp + " Message: " + midiMessageToString(sMessage));
                                    if(_instrumentSelectingTrack != null)
                                    {
                                        if(sMessage.getCommand() == ShortMessage.NOTE_ON && sMessage.getData2() != 0)
                                        {
                                            _instrumentSelectingTrack.setNote(sMessage.getData1());
                                        }
                                    }
                                }
                            }
                            
                            private String midiMessageToString(ShortMessage sMessage)
                            {
                                return "Channel: " + sMessage.getChannel() + ", Command: " + sMessage.getCommand() + ", Data1: " + sMessage.getData1() + ", Data2: " + sMessage.getData2() + ", Length: " + sMessage.getLength() + ", Status: " + sMessage.getStatus();
                            }

                            @Override
                            public void close()
                            {
                            }
                        });
                    }
                    catch (MidiUnavailableException exc)
                    {
                        exc.printStackTrace();
                        _midiInDevice = null;
                    } 
                }
            }
        }
        
        public void setSelectingTrack(TrackModel intstrumentSelectingTrack)
        {
            _instrumentSelectingTrack = intstrumentSelectingTrack;
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
            inputState.instrumentSelectedPressed();
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
            inputState.channelSelectPressed();
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
        public void mousePressed(MouseEvent event, InputState inputState)
        {
            inputState.noteSelected();
        }

        @Override
        protected void buttonPressed(InputState inputState)
        {
        }

        @Override
        protected void setColor()
        {
            fill(255, 128, 255);
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
