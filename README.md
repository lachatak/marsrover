# Mars Rover #

A squad of robotic rovers are to be landed by NASA on a plateau on Mars.
This plateau, which is curiously rectangular, must be navigated by the
rovers so that their on-board cameras can get a complete view of the
surrounding terrain to send back to Earth.
A rover's position and location is represented by a combination of x and y
co-ordinates and a letter representing one of the four cardinal compass
points. The plateau is divided up into a grid to simplify navigation. An
example position might be 0, 0, N, which means the rover is in the bottom
left corner and facing North.
In order to control a rover, NASA sends a simple string of letters. The
possible letters are 'L', 'R' and 'M'. 'L' and 'R' makes the rover spin 90
degrees left or right respectively, without moving from its current spot.
'M' means move forward one grid point, and maintain the same heading.
Assume that the square directly North from (x, y) is (x, y+1).

### INPUT ###
The first line of input is the upper-right coordinates of the plateau, the
lower-left coordinates are assumed to be 0,0.
The rest of the input is information pertaining to the rovers that have
been deployed. Each rover has two lines of input. The first line gives the
rover's position, and the second line is a series of instructions telling
the rover how to explore the plateau.
The position is made up of two integers and a letter separated by spaces,
corresponding to the x and y co-ordinates and the rover's orientation.
Each rover will be finished sequentially, which means that the second rover
won't start to move until the first one has finished moving.

### OUTPUT ###
The output for each rover should be its final co-ordinates and heading.

### INPUT AND OUTPUT ###
Test Input:
```
5 5
1 2 N
LMLMLMLMM
3 3 E
MMRMMRMRRM
```

Expected Output:
```
1 3 N
5 1 E
```

## Extended requirements ##

After parsing the configuration the rovers should move simultaneously.
Every action type has its own required time. It makes the simulation more realistic.
It is possible that two rovers collide. In this case both of them should break down and won't react on any subsequent commands.
If a rover leaves the plateau it disappears and doesn't answer for instructions.
After the mars rover expedition NASA should provide a report how the expedition ended. It is just a simple textual report summarizing the result. Was it successful or something unexpected happened like collusion or some rovers are missing.

## Running the app ##

After you have cloned the project there are 3 separate projects. All you have to do is just start sbt and deploy the app locally.
```
sbt
publishLocal
```

After deploying you are ready to run the application. By default it is configured to expect 3 mars rovers to support the expedition. The configuration can be found under the marsrover project's resources.
It means that aou have to start 3 rovers in a separate JVM. To make it easy I added 3 aliases.
- *hq* starts the server. This should be used first.
- *rover* start a rover. We need 3 separate console to run them. After you have started you have to provide an ide. It is going to distinguish between the rovers on the UI.
- *ui* start a play framework which is used to track the rover movements.

### Running on different hosts ###

If you would like to try the app in a distributed fashion all you need is to modify the marsrover and marsroverpay configuration to point where the servers is running. All the rest is the same.
Components are going to find each other in the clustered environment.

If everything was OK when you start your UI you should see the moving rovers like this:

![Alt text](https://github.com/lachatak/marsrover/tree/play/screenshot.png "Running rovers")

Have a good fun!






