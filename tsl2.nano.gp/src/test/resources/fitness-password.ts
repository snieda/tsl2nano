function fitness() {
    var expected = "dasistmeinpass1!wort";
    // if (input.length != expected.length)
    //     return 999999999.0;
    var expectedLong = new Array(input.length);
    for (i = 0; i< expected; i++)
        expectedLong[i] = expected.charCodeAt(i);
    var sum = 0;
    for(i=0; i<expectedLong.length; i++) {
        sum += (expectedLong[i] - (i<input.length ? input[i] : 0));
    }
    return  Math.abs(sum);
}

fitness();