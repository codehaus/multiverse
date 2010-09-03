How to install:

You need to install gradle 0.9-RC1

http://www.gradle.org

And execute the following command

gradle install

The jar will be stored in build/libs

The Multiverse jar has no external dependencies. It uses velocity (pre) compiletime to generate Java sources, but
there is no runtime dependency on Velocity or any other library.