
fun main(args: Array<String>) {
    day1()
}


fun day1(){
    val input: List<Int> = listOf( 1721,979,366,299,675,1456 )
    var inputMap = mutableMapOf<Int,Int>()

    for(number in input){
        if(inputMap.contains(2020- number) == false){
            println("${2020-number}, $number")
            inputMap.put(number, 1)
        }else {

            println((2020-number) * number)
            break;
        }
    }
}