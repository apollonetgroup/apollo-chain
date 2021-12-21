contract InfoFeed {
function d1(uint x1) public{

        assembly{
            function f(x) -> y { switch x case 0 { y := 1 } default { y := mul(x, f(sub(x, 1))) } }
        }
    }
    function d2(uint x1) public{
        assembly {
            let x:=1
            x := mul(1, add(2, 3))}
    }
    function f(uint x) public{
        assembly { x := sub(x, 1) }

    }
    // 0.6.0 Variable declarations cannot shadow declarations outside the assembly block.
    function d(uint x1) public returns(uint256){
        uint256 x;
        assembly{
            x := add(2, 3)
            let y := mload(0x40)
            x := add(x, y)
        }
        return x;
    }
    function d4(uint x) public{
        // Error: The labels 'repeat' is disallowed. Please use "if", "switch", "for" or function calls instead
        //assembly{let x := 10 repeat: x := sub(x, 1) jumpi(repeat, eq(x, 0))
        x = x;
        //}
    }
    function d5(uint x1) public{
        assembly{
            function f(x) -> y { switch x case 0 { y := mul(x, 2) } default { y := 0 } }

        }
     }

     function d6(uint x1) public{
        assembly{
            function f(x) -> y { for { let i := 0 } lt(i, x) { i := add(i, 1) } { y := mul(2, y) } }
        }
    }
    } 